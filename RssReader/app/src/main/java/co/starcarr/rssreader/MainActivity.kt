package co.starcarr.rssreader

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.starcarr.rssreader.adapter.ArticleAdapter
import co.starcarr.rssreader.adapter.ArticleLoader
import co.starcarr.rssreader.producer.ArticleProducer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ArticleLoader {

    private lateinit var articles: RecyclerView
    private lateinit var viewAdapter: ArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewAdapter = ArticleAdapter(this)
        articles = findViewById<RecyclerView>(R.id.articles).apply {
            adapter = viewAdapter
        }

        GlobalScope.launch {
            loadMore()
        }

    }

    override suspend fun loadMore() {
        val producer = ArticleProducer.producer

        if (!producer.isClosedForReceive) {
            val articles = producer.receive()

            MainScope().launch {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                viewAdapter.add(articles)
                Log.d("Main", "Currently has ${viewAdapter.itemCount} articles")
            }
        }
    }

}
