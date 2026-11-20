package kz.alnazar.schedule.imp

import android.graphics.Bitmap
import android.util.Base64
import kz.alnazar.schedule.data.Bell
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Распознавание расписания через Gemini API (нужен интернет и ключ из Google AI Studio). */
object Gemini {
    private val MODELS = listOf("gemini-flash-latest", "gemini-2.5-flash", "gemini-2.0-flash")

    private const val PROMPT = """На фото школьное расписание уроков (таблица: дни недели и номера уроков). Язык — русский и/или казахский.
Верни JSON: days — массив дней по порядку (пн, вт, ср, чт, пт и, если есть, сб; пустой день — пустой массив).
В каждом дне lessons — массив уроков строго по номерам урока сверху вниз. Если урока в ячейке нет (окно), subject — пустая строка.
subject — название предмета как на фото, но без кабинета и фамилии учителя. room — номер кабинета (например 313 или Спортзал), если указан, иначе пустая строка.
bells — массив звонков по номерам уроков {start:"ЧЧ:ММ", end:"ЧЧ:ММ"}, если время уроков есть на фото, иначе пустой массив.
Ничего не выдумывай и не исправляй. Только JSON."""

    private fun schema(): JSONObject = JSONObject("""{
      "type":"OBJECT","properties":{
        "days":{"type":"ARRAY","items":{"type":"OBJECT","properties":{"lessons":{"type":"ARRAY","items":{"type":"OBJECT","properties":{"subject":{"type":"STRING"},"room":{"type":"STRING"}},"required":["subject","room"]}}},"required":["lessons"]}},
        "bells":{"type":"ARRAY","items":{"type":"OBJECT","properties":{"start":{"type":"STRING"},"end":{"type":"STRING"}},"required":["start","end"]}}
      },"required":["days","bells"]}""")

    class Err(val code: Int, msg: String) : Exception(msg)

    fun recognize(bmp: Bitmap, key: String): ImpResult {
        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                .put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", b64)))
                .put(JSONObject().put("text", PROMPT)))))
            put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("responseSchema", schema()).put("temperature", 0))
        }.toString()
        var last: Err? = null
        for (m in MODELS) {
            try { return parse(call(m, key, body)) } catch (e: Err) {
                last = e
                if (e.code != 404 && e.code != 400) throw e   // 404/400 — модели нет или она не подходит: пробуем следующую
            }
        }
        throw last ?: Err(0, "no model")
    }

    private fun call(model: String, key: String, body: String): String {
        val c = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.connectTimeout = 15000; c.readTimeout = 60000; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("x-goog-api-key", key.trim())
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val txt = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw Err(code, txt.take(300))
        return txt
    }

    private fun parse(resp: String): ImpResult {
        val text = JSONObject(resp).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        val o = JSONObject(text)
        val days = o.getJSONArray("days").let { a -> (0 until a.length()).map { i ->
            val ls = a.getJSONObject(i).getJSONArray("lessons")
            (0 until ls.length()).map { j -> ls.getJSONObject(j).let { ImpCell(it.optString("subject").trim(), it.optString("room").trim()) } }
        } }
        val bells = o.optJSONArray("bells")?.let { a -> (0 until a.length()).mapNotNull { i ->
            val b = a.getJSONObject(i); val s = hm(b.optString("start")); val e = hm(b.optString("end"))
            if (s != null && e != null && e > s) Bell(s, e) else null
        } } ?: emptyList()
        return ImpResult(days, bells)
    }

    fun hm(s: String): Int? {
        val m = Regex("""(\d{1,2})[:.\-](\d{2})""").find(s) ?: return null
        val h = m.groupValues[1].toInt(); val mi = m.groupValues[2].toInt()
        return if (h in 0..23 && mi in 0..59) h * 60 + mi else null
    }
}
