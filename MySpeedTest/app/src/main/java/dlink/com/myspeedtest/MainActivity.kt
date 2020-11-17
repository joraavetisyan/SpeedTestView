package dlink.com.myspeedtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import dlink.com.myspeedtest.gauge.GaugeView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val gaugeView: GaugeView = findViewById<View>(R.id.gauge_view) as GaugeView
        val btnStart: Button = findViewById<View>(R.id.btnStart) as Button
        gaugeView.setShowRangeValues(true)
        gaugeView.setTargetValue(0F)
        val random = Random(10)
        val timer: CountDownTimer = object : CountDownTimer(10000, 2) {
            override fun onTick(millisUntilFinished: Long) {
                gaugeView.setTargetValue(random.nextInt(100).toFloat())
            }

            override fun onFinish() {
                gaugeView.setTargetValue(0F)
            }
        }
        btnStart.setOnClickListener { timer.start() }
    }
}