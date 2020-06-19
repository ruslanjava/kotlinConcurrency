package co.starcarr.rssreader

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.starcarr.rssreader.adapter.ArticleAdapter
import co.starcarr.rssreader.search.ResultsCounter
import co.starcarr.rssreader.search.Searcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {
    private val searcher = Searcher()

    private lateinit var articles: RecyclerView
    private lateinit var viewAdapter: ArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        viewAdapter = ArticleAdapter()
        articles = findViewById<RecyclerView>(R.id.articles).apply {
            adapter = viewAdapter
        }

        findViewById<Button>(R.id.searchButton).setOnClickListener {
            viewAdapter.clear()
            GlobalScope.launch {
                ResultsCounter.reset()
                search()
            }
        }

        GlobalScope.launch {
            updateCounter()
        }
    }

    private suspend fun updateCounter() {
        val notifications = ResultsCounter.getNotificationChannel()
        val results = findViewById<TextView>(R.id.results)

        while (!notifications.isClosedForReceive) {
            val newAmount = notifications.receive()

            withContext(MainScope().coroutineContext) {
                results.text = "Results: $newAmount"
            }
        }
    }

    private suspend fun search() {
        val query = findViewById<EditText>(R.id.searchText).text.toString()

        val channel = searcher.search(query)

        while (!channel.isClosedForReceive) {
            val article = channel.receive()

            MainScope().launch {
                viewAdapter.add(article)
            }
        }
    }

}