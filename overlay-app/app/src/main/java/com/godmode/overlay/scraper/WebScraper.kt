package com.godmode.overlay.scraper

import com.godmode.overlay.util.ScrapedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class WebScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        // Rotate through common desktop user agents to avoid simple bot blocks
        .build()

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:125.0) Gecko/20100101 Firefox/125.0"
    )

    suspend fun scrape(url: String): Result<ScrapedItem> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgents.random())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("DNT", "1")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: error("Empty response")
            val doc = Jsoup.parse(html, url)

            // Remove noise
            doc.select("script, style, nav, footer, header, .ad, .ads, [class*='banner'], [id*='cookie']").remove()

            val title = doc.title().takeIf { it.isNotBlank() }
                ?: doc.select("h1").firstOrNull()?.text()
                ?: "No title"

            // Extract main text — prefer article/main, fallback to body
            val contentEl = doc.select("article, [role='main'], main").firstOrNull() ?: doc.body()
            val text = contentEl.select("p, h1, h2, h3, h4, li")
                .joinToString("\n") { it.text().trim() }
                .lines()
                .filter { it.length > 30 }
                .joinToString("\n")

            // Extract images with absolute URLs
            val images = doc.select("img[src]")
                .map { it.absUrl("src") }
                .filter { it.startsWith("http") }
                .take(20)

            ScrapedItem(url = url, title = title, text = text, images = images)
        }
    }

    // Extract structured data: prices, offers, tables
    suspend fun extractStructured(url: String): Result<Map<String, List<String>>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgents.random())
                .build()
            val html = client.newCall(request).execute().body?.string() ?: error("Empty")
            val doc = Jsoup.parse(html, url)
            doc.select("script, style").remove()

            val result = mutableMapOf<String, List<String>>()

            // Prices (common patterns)
            result["prices"] = doc.select(
                "[class*='price'],[class*='Price'],[data-price],[itemprop='price']," +
                "[class*='cost'],[class*='amount'],[class*='offer'],[class*='deal']"
            ).map { it.text().trim() }.filter { it.isNotBlank() }.distinct()

            // Offers / promos
            result["offers"] = doc.select(
                "[class*='promo'],[class*='deal'],[class*='offer'],[class*='discount']," +
                "[class*='coupon'],[class*='sale'],[class*='badge']"
            ).map { it.text().trim() }.filter { it.isNotBlank() }.distinct()

            // Tables
            result["tables"] = doc.select("table").map { table ->
                table.select("tr").joinToString(" | ") { row ->
                    row.select("td,th").joinToString(" | ") { it.text().trim() }
                }
            }.filter { it.isNotBlank() }

            // Hidden / data attributes that might expose promo codes / internal values
            result["data_attributes"] = doc.select("[data-*]")
                .flatMap { el ->
                    el.attributes()
                        .filter { attr -> attr.key.startsWith("data-") && attr.value.isNotBlank() }
                        .map { "${it.key}=${it.value}" }
                }
                .filter { it.length < 200 }
                .distinct()
                .take(50)

            result
        }
    }

    // Parse raw HTML string (offline — no network call)
    suspend fun extractFromHtml(html: String): Map<String, List<String>> = withContext(Dispatchers.Default) {
        val doc = Jsoup.parse(html)
        doc.select("script[src], style, link[rel=stylesheet]").remove()

        val result = mutableMapOf<String, List<String>>()

        result["prices"] = doc.select(
            "[class*='price'],[class*='Price'],[data-price],[itemprop='price']," +
            "[class*='cost'],[class*='amount'],[class*='offer'],[class*='deal']"
        ).map { it.text().trim() }.filter { it.isNotBlank() }.distinct()

        result["offers"] = doc.select(
            "[class*='promo'],[class*='deal'],[class*='offer'],[class*='discount']," +
            "[class*='coupon'],[class*='sale'],[class*='badge'],[class*='reward']," +
            "[class*='incentive'],[class*='bonus']"
        ).map { it.text().trim() }.filter { it.isNotBlank() }.distinct()

        // Pull inline JSON blobs out of <script> tags — often contains full API responses
        result["json_blobs"] = doc.select("script:not([src])").mapNotNull { el ->
            val txt = el.data().trim()
            // Look for JSON objects/arrays embedded in JS variables or window.__INITIAL_STATE__ etc.
            val jsonRegex = Regex("""(?:=\s*|:\s*)(\{[\s\S]{50,}?\}|\[[\s\S]{20,}?\])""")
            jsonRegex.findAll(txt).mapNotNull { match ->
                val candidate = match.groupValues[1]
                runCatching {
                    if (candidate.startsWith("{")) org.json.JSONObject(candidate).toString(2)
                    else org.json.JSONArray(candidate).toString(2)
                }.getOrNull()
            }.firstOrNull()
        }.distinct().take(20)

        result["tables"] = doc.select("table").map { table ->
            table.select("tr").joinToString("\n") { row ->
                row.select("td,th").joinToString(" | ") { it.text().trim() }
            }
        }.filter { it.isNotBlank() }

        result["data_attributes"] = doc.select("[data-*]")
            .flatMap { el ->
                el.attributes()
                    .filter { attr -> attr.key.startsWith("data-") && attr.value.isNotBlank() }
                    .map { "${it.key}=${it.value}" }
            }
            .filter { it.length < 300 }
            .distinct()
            .take(100)

        result
    }
}
