package cat.jason.memoryanalysis

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cat.jason.memoryanalysis.ui.theme.MemoryAnalysisTheme
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var composeViewJava: ComposeView

    private val params = WindowManager.LayoutParams(
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

    companion object {
        const val OVERLAY_PERMISSION_REQ_CODE = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composeViewJava = ComposeView(this)
        setContent {
            Text(text = "MainActivity")
        }

        // Trigger the floating window (for demo purposes)
        showFloatingWindow()
    }

    @Composable
    fun CircleView(
        usedJavaMemory: Float,
        maxJavaMemory: Float,
        usedCPlusMemory: Float,
        warningCPlusMemory: Float,
        javaCircleColor: Color,
        cPlusCircleColor: Color,
        composeView: ComposeView,
        onToggle: () -> Unit
    ) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        val javaProportion = usedJavaMemory / maxJavaMemory
        val cPlusProportion = usedCPlusMemory / warningCPlusMemory

        /**
         * Java
         */

        Box(
            modifier = Modifier
                .width(70.dp)
                .height(160.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onToggle
                )
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        // Update the WindowManager.LayoutParams for the floating window
                        params.x += dragAmount.x.roundToInt()
                        params.y += dragAmount.y.roundToInt()
                        windowManager.updateViewLayout(composeView, params)
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .width(70.dp)
                    .height(70.dp)
                    .border(2.dp, Color.Black, CircleShape)
            ) {
                // Drawing the portion of used memory
                drawArc(
                    color = javaCircleColor,
                    startAngle = -90f,
                    sweepAngle = 360 * javaProportion,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 25.dp)
            ) {
                Text(
                    text = "$usedJavaMemory MB",
                    color = Color.Black,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                        .padding(2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 50.dp)
            ) {
                Text(
                    text = "Java",
                    color = Color.Black,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                        .padding(2.dp)
                )
            }

            /**
             * C++
             */

            Canvas(
                modifier = Modifier
                    .offset(y = 90.dp)
                    .width(70.dp)
                    .height(70.dp)
                    .border(2.dp, Color.Black, CircleShape)
            ) {
                // Drawing the portion of used memory
                drawArc(
                    color = cPlusCircleColor,
                    startAngle = -90f,
                    sweepAngle = 360 * cPlusProportion,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-25).dp)
            ) {
                Text(
                    text = "$usedCPlusMemory MB",
                    color = Color.Black,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                        .padding(2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp)
            ) {
                Text(
                    text = "C++",
                    color = Color.Black,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                        .padding(2.dp)
                )
            }
        }
    }


    @Composable
    fun FloatingContent(
        minimized: Boolean,
        composeView: ComposeView,
        onToggle: () -> Unit
    ) {
        var usedJavaMemory by remember { mutableStateOf(0f) } // Mutable state for the used memory
        var usedCPlusMemory by remember { mutableStateOf(0f) } // Mutable state for the used memory


        // Recalculate memory usage every second
        LaunchedEffect(key1 = "memoryUpdate") {
            while (true) {
                usedJavaMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
                    .freeMemory()) / 1048576f  // in MB

                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                val nativePrivateKB = memoryInfo.nativePrivateDirty.toFloat()
                usedCPlusMemory = nativePrivateKB / 1024
                delay(1000) // Wait for a second
            }
        }

        val maxMemory = Runtime.getRuntime().maxMemory() / 1048576f
        val warningCPlusMemory = 500f

        val javaCircleColor = when {
            usedJavaMemory <= maxMemory / 3 -> Color.Green
            usedJavaMemory <= 2 * maxMemory / 3 -> Color.Blue
            else -> Color.Red
        }

        val cPlusCircleColor = when {
            usedCPlusMemory <= warningCPlusMemory / 3 -> Color.Green
            usedCPlusMemory <= 2 * warningCPlusMemory / 3 -> Color.Blue
            else -> Color.Red
        }

        if (minimized) {
            // The entire circle acts as the "Maximize" button
            CircleView(
                usedJavaMemory = (usedJavaMemory * 10).toInt() / 10f,
                maxJavaMemory = maxMemory,
                usedCPlusMemory = (usedCPlusMemory * 10).toInt() / 10f,
                warningCPlusMemory = warningCPlusMemory,
                javaCircleColor = javaCircleColor,
                cPlusCircleColor = cPlusCircleColor,
                composeView = composeView,
                onToggle = onToggle
            )
        } else {
            // Your existing full window content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BarChartComposable(Modifier.padding(top = 40.dp))
                CMemoryLineChartComposable(Modifier.padding(top = 40.dp))

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
                val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
                    .freeMemory()) / 1048576// in MB
                val maxMemory = Runtime.getRuntime().maxMemory() / 1048576 // in MB

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

                    val maxMemoryValue = Runtime.getRuntime().maxMemory() / 1048576 // in MB
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

    @Composable
    fun CMemoryLineChartComposable(modifier: Modifier = Modifier) {
        val entries = remember { mutableStateListOf<Entry>() }
        val colors = remember { mutableStateListOf<Int>() }
        val warningMemory = 200

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000) // Wait for a second

                // Use Debug.MemoryInfo to get nativePrivateDirty
                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                val nativePrivateKB = memoryInfo.nativePrivateDirty
                val usedMemory = nativePrivateKB.toFloat() / 1024  // Convert KB to MB

                when {
                    usedMemory <= warningMemory / 3 -> colors.add(Color.Green.toArgb())
                    usedMemory <= 2 * warningMemory / 3 -> colors.add(Color.Blue.toArgb())
                    else -> colors.add(Color.Red.toArgb())
                }

                if (entries.size < 10) {
                    entries.add(Entry(entries.size.toFloat(), usedMemory))
                } else {
                    entries.removeAt(0)
                    colors.removeAt(0)
                    for (i in entries.indices) {
                        entries[i] = Entry(i.toFloat(), entries[i].y)
                    }
                    entries.add(Entry(9f, usedMemory))
                }
            }
        }

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(LocalConfiguration.current.screenHeightDp.dp / 5),
            factory = { context ->
                LineChart(context).apply {
                    val dataSet = LineDataSet(entries, "C Memory Usage (MB)").apply {
                        setColors(colors)
                    }
                    data = LineData(dataSet)

                    xAxis.setDrawGridLines(false)
                    xAxis.position = XAxis.XAxisPosition.BOTTOM

                    axisLeft.axisMinimum = 0f  // Set the y-axis minimum value to 0
                    axisLeft.axisMaximum = 200f
                    axisRight.isEnabled = false

                    xAxis.axisMinimum = 0f
                    xAxis.axisMaximum = 10f
                    xAxis.labelCount = 10

                    description.isEnabled = false

                    invalidate()
                }
            },
            update = { chart ->
                val dataSet = LineDataSet(entries, "C Memory Usage (MB)").apply {
                    setColors(colors)
                }
                chart.data = LineData(dataSet)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun displayFloatingWindow(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            composeViewJava.setViewTreeSavedStateRegistryOwner(this@MainActivity)
            composeViewJava.setViewTreeLifecycleOwner(this@MainActivity)

            composeViewJava.setContent {
                val minimized = remember { mutableStateOf(false) }
                MemoryAnalysisTheme {
                    FloatingContent(
                        minimized = minimized.value,
                        composeView = composeViewJava,
                        onToggle = { minimized.value = !minimized.value }
                    )
                }
            }

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.addView(composeViewJava, params)
        }
    }

    override fun onStop() {
        super.onStop()
        composeViewJava.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        composeViewJava.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        composeViewJava.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            showFloatingWindow()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
