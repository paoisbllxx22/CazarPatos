package paola.gamboa.cazarpatos

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var textViewUser: TextView
    private lateinit var textViewCounter: TextView
    private lateinit var textViewTime: TextView
    private lateinit var imageViewDuck: ImageView
    private lateinit var soundPool: SoundPool

    // Manejador para retrasar la restauración de la imagen original
    private val handler = Handler(Looper.getMainLooper())
    private var counter = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private var soundId: Int = 0
    private var isLoaded = false
    private var gameOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
        //Inicialización de variables
        textViewUser = findViewById(R.id.textViewUser)
        textViewCounter = findViewById(R.id.textViewCounter)
        textViewTime = findViewById(R.id.textViewTime)
        imageViewDuck = findViewById(R.id.imageViewDuck)

        //Obtener el usuario de pantalla login
        val extras = intent.extras ?: return
        var usuario = extras.getString(EXTRA_LOGIN) ?: "Unknown"
        usuario = usuario.substringBefore("@")
        textViewUser.setText(usuario)
        //Determina el ancho y largo de pantalla
        initializeScreen()
        //Cuenta regresiva del juego
        initializeCountdown()
        // Configuración del SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(10) // Puedes reproducir hasta 10 sonidos a la vez
            .build()

        // Cargar el sonido
        soundId = soundPool.load(this, R.raw.gunshot, 1)

        // Listener cuando el sonido está cargado
        soundPool.setOnLoadCompleteListener { _, _, _ ->
            isLoaded = true
        }
        imageViewDuck.setOnClickListener {
            if (gameOver) return@setOnClickListener
            counter++
            if (isLoaded) {
                soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
            }
            textViewCounter.setText(counter.toString())
            imageViewDuck.setImageResource(R.drawable.duck_clicked)

            // Restaurar la imagen original después de 500ms
            handler.postDelayed({
                imageViewDuck.setImageResource(R.drawable.duck)
            }, 500)
            moveDuck()
        }
    }

    private fun initializeScreen() {
        // 1. Obtenemos el tamaño de la pantalla del dispositivo
        val display = this.resources.displayMetrics
        screenWidth = display.widthPixels
        screenHeight = display.heightPixels
    }

    private fun moveDuck() {
        when (Random.nextInt(3)) {
            0 -> moveDuckRandomly()
            1 -> moveDuckRandomlyWithAnimation()
            2 -> moveDuckRandomlyWithParabola()
        }
    }

    private fun moveDuckRandomly() {
        val min = imageViewDuck.getWidth() / 2
        val maximoX = screenWidth - imageViewDuck.getWidth()
        val maximoY = screenHeight - imageViewDuck.getHeight()
        // Generamos 2 números aleatorios, para la coordenadas x , y
        val randomX = Random.nextInt(0, maximoX - min + 1)
        val randomY = Random.nextInt(112 + imageViewDuck.getHeight(), maximoY - min + 1)

        imageViewDuck.x = randomX.toFloat()
        imageViewDuck.y = randomY.toFloat()
    }

    private fun moveDuckRandomlyWithAnimation() {
        val min = imageViewDuck.getWidth() / 2
        val maximoX = screenWidth - imageViewDuck.getWidth()
        val maximoY = screenHeight - imageViewDuck.getHeight()
        // Generamos 2 números aleatorios, para la coordenadas x , y
        val randomX = Random.nextInt(0, maximoX - min + 1)
        val randomY = Random.nextInt(112 + imageViewDuck.getHeight(), maximoY - min + 1)

        imageViewDuck.animate()
            .x(randomX.toFloat())
            .y(randomY.toFloat())
            .setDuration(300) // animación suave
            .start()
    }

    private fun moveDuckRandomlyWithParabola() {
        val maxX = screenWidth - imageViewDuck.width
        val maxY = screenHeight - imageViewDuck.height

        if (maxX > 0 && maxY > 0) {
            val startX = imageViewDuck.x
            val startY = imageViewDuck.y

            val endX = Random.nextInt(0, maxX).toFloat()
            val endY = Random.nextInt(112 + imageViewDuck.getHeight(), maxY).toFloat()

            val duration = 300L

            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = duration
            animator.interpolator = LinearInterpolator()

            animator.addUpdateListener { animation ->
                val t = animation.animatedValue as Float

                // Interpolación lineal en X
                val currentX = startX + (endX - startX) * t

                // Movimiento parabólico en Y: fórmula cuadrática invertida
                val arcHeight = 300f // altura máxima de la parábola
                val midT = t * (1 - t) * 4 // parábola que sube y baja

                val currentY = startY + (endY - startY) * t - arcHeight * midT

                imageViewDuck.x = currentX
                imageViewDuck.y = currentY
            }

            animator.start()
        }
    }

    private var countDownTimer = object : CountDownTimer(20000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val secondsRemaining = millisUntilFinished / 1000
            textViewTime.setText("${secondsRemaining}s")
        }

        override fun onFinish() {
            textViewTime.setText("0s")
            gameOver = true
            showGameOverDialog()
            val nombreJugador = textViewUser.text.toString()
            val patosCazados = textViewCounter.text.toString()
            procesarPuntajePatosCazados(nombreJugador, patosCazados.toInt()) //Firestore

        }
    }

    private fun initializeCountdown() {
        countDownTimer.start()
    }

    private fun showGameOverDialog() {
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage(getString(R.string.dialog_message_congratulations, counter))
            .setTitle(getString(R.string.dialog_title_game_end))
            .setIcon(R.drawable.duck)
            .setPositiveButton(getString(R.string.button_restart)) { _, _ ->
                restartGame()
            }
            .setNegativeButton(getString(R.string.button_close)) { _, _ ->
                // Dialog dismisses automatically
            }
            .setCancelable(false)  // Prevents closing on outside click
        builder.create().show()
    }

    private fun restartGame() {
        counter = 0
        gameOver = false
        countDownTimer.cancel()
        textViewCounter.setText(counter.toString())
        moveDuck()
        initializeCountdown()
    }

    override fun onStop() {
        Log.w(EXTRA_LOGIN, "Play canceled")
        countDownTimer.cancel()
        textViewTime.text = "0s"
        gameOver = true
        soundPool.stop(soundId)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    fun jugarOnline() {
        var intentWeb = Intent()
        intentWeb.action = Intent.ACTION_VIEW
        intentWeb.data = Uri.parse("https://duckhuntjs.com/")
        startActivity(intentWeb)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_nuevo_juego -> {
                restartGame()
                true
            }

            R.id.action_jugar_online -> {
                jugarOnline()
                true
            }

            R.id.action_ranking -> {
                val intent = Intent(this, RankingActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.action_salir -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun procesarPuntajePatosCazados(nombreJugador:String, patosCazados:Int){
        val jugador = Player(nombreJugador,patosCazados)
        //Trata de obtener id del documento del ranking específico,
        // si lo obtiene lo actualiza, caso contrario lo crea
        val db = Firebase.firestore
        db.collection("ranking")
            .whereEqualTo("username", jugador.username)
            .get()
            .addOnSuccessListener { documents ->
                if(documents!= null &&
                    documents.documents != null &&
                    documents.documents.count()>0
                ){
                    val idDocumento = documents.documents.get(0).id
                    val jugadorLeido = documents.documents.get(0).toObject(Player::class.java)
                    if(jugadorLeido!!.huntedDucks < patosCazados )
                    {
                        Log.w(EXTRA_LOGIN, "Puntaje actual mayor, por lo tanto actualizado")
                        actualizarPuntajeJugador(idDocumento, jugador)
                    }
                    else{
                        Log.w(EXTRA_LOGIN, "No se actualizo puntaje, por ser menor al actual")
                    }
                }
                else{
                    ingresarPuntajeJugador(jugador)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(EXTRA_LOGIN, "Error getting documents", exception)
                Toast.makeText(this, "Error al obtener datos de jugador", Toast.LENGTH_LONG).show()
            }
    }
    fun ingresarPuntajeJugador(jugador:Player){
        val db = Firebase.firestore
        db.collection("ranking")
            .add(jugador)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this,"Puntaje usuario ingresado exitosamente", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Log.w(EXTRA_LOGIN, "Error adding document", exception)
                Toast.makeText(this,"Error al ingresar el puntaje", Toast.LENGTH_LONG).show()
            }
    }
    fun actualizarPuntajeJugador(idDocumento:String, jugador:Player){
        val db = Firebase.firestore
        db.collection("ranking")
            .document(idDocumento)
            //.update(contactoHashMap)
            .set(jugador) //otra forma de actualizar
            .addOnSuccessListener {
                Toast.makeText(this,"Puntaje de usuario actualizado exitosamente", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Log.w(EXTRA_LOGIN, "Error updating document", exception)
                Toast.makeText(this,"Error al actualizar el puntaje" , Toast.LENGTH_LONG).show()
            }
    }


    fun eliminarPuntajeJugador(idDocumentoSeleccionado:String){
        val db = Firebase.firestore
        db.collection("ranking")
            .document(idDocumentoSeleccionado)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this,"Puntaje de usuario eliminado exitosamente", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Log.w(EXTRA_LOGIN, "Error deleting document", exception)
                Toast.makeText(this,"Error al eliminar el puntaje" , Toast.LENGTH_LONG).show()
            }
    }

}
