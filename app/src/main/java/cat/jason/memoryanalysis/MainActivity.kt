package cat.jason.memoryanalysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cat.jason.memoryanalysis.ui.theme.MemoryAnalysisTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoryAnalysisTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LineChartComposable(Modifier.padding(top = 40.dp)) // 添加这一行
                    }
                }
            }
        }

    }
}

@Composable
fun LineChartComposable(modifier: Modifier = Modifier) {
    val entries = remember { mutableStateListOf<Entry>() }

        // 使用 LaunchedEffect 和 delay 函数来每秒新增数据
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 等待一秒

            // 新增数据
            if (entries.size < 10) {
                entries.add(Entry((entries.size + 1).toFloat(), (Math.random() * 50).toFloat()))
            } else {
                // 移除第一个数据点，然后添加新的数据点
                entries.removeAt(0)
                // 更新列表中所有数据点的x值，使它们向左移动
                for (i in entries.indices) {
                    entries[i] = Entry(i.toFloat(), entries[i].y)
                }
                // 添加新的数据点到列表末尾
                entries.add(Entry(9f, (Math.random() * 50).toFloat()))
            }
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(LocalConfiguration.current.screenHeightDp.dp / 10),
        factory = { context ->
            LineChart(context).apply {
                val dataSet = LineDataSet(entries, "Sample Data").apply {
                    color = Color.Red.toArgb()
                }
                data = LineData(dataSet)

                xAxis.setDrawGridLines(false)  // 去掉X轴的网格线
                axisLeft.setDrawGridLines(false)  // 去掉左Y轴的网格线
                axisRight.setDrawGridLines(false)  // 去掉右Y轴的网格线

                invalidate()
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, "Sample Data").apply {
                color = Color.Red.toArgb()
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}