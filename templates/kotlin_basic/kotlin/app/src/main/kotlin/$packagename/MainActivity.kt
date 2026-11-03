package $packagename

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "¡Hola desde Kotlin en CodeAssist!"
        textView.textSize = 24f
        textView.gravity = Gravity.CENTER
        
        setContentView(textView)
    }
}
