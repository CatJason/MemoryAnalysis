package cat.jason.memoryanalysis

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cat.jason.memoryanalysis.ui.theme.MemoryAnalysisTheme
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var composeView: ComposeView

    companion object{
        const val OVERLAY_PERMISSION_REQ_CODE = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composeView = ComposeView(this)
        setContent {
            Text(text = "MainActivity")
        }

        // Trigger the floating window (for demo purposes)
        showFloatingWindow()
    }

    @Composable
    fun FloatingContent(minimized: Boolean, onToggle: () -> Unit) {
        var usedMemory by remember { mutableStateOf(0L) } // Mutable state for the used memory

        // Recalculate memory usage every second
        LaunchedEffect(key1 = "memoryUpdate") {
            while (true) {
                usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) // in MB
                delay(1000) // Wait for a second
            }
        }

        val maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024) // in MB

        val circleColor = when {
            usedMemory <= maxMemory / 3 -> Color.Green
            usedMemory <= 2 * maxMemory / 3 -> Color.Blue
            else -> Color.Red
        }

        if (minimized) {
            // The entire circle acts as the "Maximize" button
            Surface(
                modifier = Modifier.size(50.dp).clickable(onClick = onToggle),
                shape = CircleShape,
                color = circleColor
            ) {
                // Use Box to center the Text content
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$usedMemory MB",
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Your existing full window content
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BarChartComposable(Modifier.padding(top = 40.dp))

                Spacer(modifier = Modifier.height(16.dp)) // Adds spacing between the chart and the button

                Button(onClick = {
                    Runtime.getRuntime().gc()
                }) {
                    Text("Run Garbage Collection")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onToggle) {
                    Text("Minimize")
                }
            }
        }
    }

    @Composable
    fun BarChartComposable(modifier: Modifier = Modifier) {
        val entries = remember { mutableStateListOf<BarEntry>() }
        val colors = remember { mutableStateListOf<Int>() }

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000) // Wait for a second

                // Calculate the used memory
                val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)// in MB
                val maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024) // in MB

                when {
                    usedMemory <= maxMemory / 3 -> colors.add(Color.Green.toArgb())
                    usedMemory <= 2 * maxMemory / 3 -> colors.add(Color.Blue.toArgb())
                    else -> colors.add(Color.Red.toArgb())
                }

                if (entries.size < 10) {
                    entries.add(BarEntry(entries.size.toFloat(), usedMemory.toFloat()))
                } else {
                    entries.removeAt(0)
                    colors.removeAt(0)
                    for (i in entries.indices) {
                        entries[i] = BarEntry(i.toFloat(), entries[i].y)
                    }
                    entries.add(BarEntry(19f, usedMemory.toFloat()))
                }
            }
        }

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(LocalConfiguration.current.screenHeightDp.dp / 5),
            factory = { context ->
                BarChart(context).apply {
                    val dataSet = BarDataSet(entries, "Java Memory Usage (MB)").apply {
                        setColors(colors)
                    }
                    data = BarData(dataSet).apply {
                        barWidth = 1f
                    }

                    xAxis.setDrawGridLines(false)
                    xAxis.position = XAxis.XAxisPosition.BOTTOM

                    val maxMemoryValue = Runtime.getRuntime().maxMemory() / (1024 * 1024) // in MB
                    axisLeft.axisMaximum = maxMemoryValue.toFloat()
                    axisRight.isEnabled = false

                    xAxis.axisMinimum = 0f
                    xAxis.axisMaximum = 10f
                    xAxis.labelCount = 10

                    description.isEnabled = false  // This line disables the description label

                    invalidate()
                }
            },
            update = { chart ->
                val dataSet = BarDataSet(entries, "Java Memory Usage (MB)").apply {
                    setColors(colors)
                }
                chart.data = BarData(dataSet).apply {
                    barWidth = 1f
                }
                chart.invalidate()
            }
        )
    }

    private fun showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            } else {
                displayFloatingWindow(this)
            }
        } else {
            displayFloatingWindow(this)
        }
    }

    private fun displayFloatingWindow(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            // Manually set ViewTreeLifecycleOwner and ViewTreeViewModelStoreOwner
            composeView.setViewTreeSavedStateRegistryOwner(this@MainActivity)
            composeView.setViewTreeLifecycleOwner(this@MainActivity)

            composeView.setContent {
                val minimized = remember { mutableStateOf(false) }
                MemoryAnalysisTheme {
                    FloatingContent(
                        minimized = minimized.value,
                        onToggle = { minimized.value = !minimized.value }
                    )
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.addView(composeView, params)
        }
    }

    override fun onStop() {
        super.onStop()
        composeView.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        composeView.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            showFloatingWindow()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
