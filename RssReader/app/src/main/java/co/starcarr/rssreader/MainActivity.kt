package co.starcarr.rssreader

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.starcarr.rssreader.adapter.ArticleAdapter
import co.starcarr.rssreader.model.Article
import co.starcarr.rssreader.model.Feed
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {

    private val feeds = listOf(
            Feed("npr", "https://www.npr.org/rss/rss.php?id=1001"),
            Feed("cnn", "http://rss.cnn.com/rss/cnn_topstories.rss"),
            Feed("fox", "http://feeds.foxnews.com/foxnews/latest?format=xml"),
            Feed("inv", "htt:myNewsFeed")
    )

    @ObsoleteCoroutinesApi
    private val dispatcher = newFixedThreadPoolContext(2, "IO")
    private val factory = DocumentBuilderFactory.newInstance()

    private lateinit var articles: RecyclerView
    private lateinit var viewAdapter: ArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewAdapter = ArticleAdapter()
        articles = findViewById<RecyclerView>(R.id.articles).apply {
            adapter = viewAdapter
        }

        asyncLoadNews()
    }

    private fun asyncLoadNews() = GlobalScope.launch {
        val requests = mutableListOf<Deferred<List<Article>>>()

        feeds.mapTo(requests) {
            asyncFetchArticles(it, dispatcher)
        }

        requests.forEach {
            it.join()
        }

        // If the code below crashes, please change the filter
        // to !it.isCompletedExceptionally until this issue is
        // closed: https://github.com/Kotlin/kotlinx.coroutines/issues/220
        val articles = requests
                .filter { !it.isCancelled }
                .flatMap { it.await() }

        // If the counter doesn't work, please change the filter
        // to it.isCompletedExceptionally until this issues is
        // closed: https://github.com/Kotlin/kotlinx.coroutines/issues/220
        val failed = requests
                .filter { it.isCancelled }
                .size

        val obtained = requests.size - failed

        MainScope().launch {
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            viewAdapter.add(articles)
        }
    }


    private fun asyncFetchArticles(feed: Feed, dispatcher: CoroutineDispatcher) = GlobalScope.async(dispatcher) {
        delay(1500)
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed.url)
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
                .map { news.childNodes.item(it) }
                .filter { Node.ELEMENT_NODE == it.nodeType }
                .map { it as Element }
                .filter { "item" == it.tagName }
                .map {
                    val title = it.getElementsByTagName("title")
                            .item(0)
                            .textContent
                    var summary = it.getElementsByTagName("description")
                            .item(0)
                            .textContent

                    if (!summary.startsWith("<div")
                            && summary.contains("<div")) {
                        summary = summary.substring(0, summary.indexOf("<div"))
                    }


                    Article(feed.name, title, summary)
                }
    }

}
