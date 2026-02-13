package com.duddleTech.convoAI

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.android.billingclient.api.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.net.URLEncoder

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, PurchasesUpdatedListener {

    companion object {
        init { System.loadLibrary("native-lib") }
        const val SUBSCRIPTION_ID = "pro_monthly_9"
        const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    private external fun getApiKey(): String

    private var coinsPerImage: Int = 5
    private var coinsRewardAd: Int = 30

    private lateinit var auth: FirebaseAuth
    private lateinit var billingClient: BillingClient
    private var isProUser = false
    private var rewardedAd: RewardedAd? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var btnTopNewChat: ImageButton
    private lateinit var toolbarCoinChip: LinearLayout
    private lateinit var toolbarCoinText: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var suggestionsRecyclerView: RecyclerView
    private lateinit var userInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var micButton: ImageButton
    private lateinit var btnImageMode: ImageButton
    private lateinit var btnAttachImage: ImageButton
    private lateinit var imagePreviewContainer: FrameLayout
    private lateinit var imagePreview: ImageView
    private lateinit var btnClosePreview: ImageButton

    private lateinit var voiceOverlay: FrameLayout
    private lateinit var voiceStatusText: TextView
    private lateinit var btnStopVoice: ImageButton
    private lateinit var voicePulse: View

    private lateinit var sidebarRecyclerView: RecyclerView
    private lateinit var btnNewChat: LinearLayout
    private lateinit var btnDarkMode: LinearLayout
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var userProfileSection: LinearLayout
    private lateinit var sidebarUserName: TextView
    private lateinit var sidebarUserEmail: TextView
    private lateinit var sidebarAvatarText: TextView
    private lateinit var sidebarAvatarImage: ImageView
    private lateinit var sidebarAvatarContainer: FrameLayout
    private lateinit var sidebarCoins: TextView
    private lateinit var btnGetCoins: LinearLayout
    private lateinit var btnGoPro: LinearLayout

    private lateinit var messageList: MutableList<Message>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var sidebarAdapter: SidebarAdapter
    private lateinit var db: AppDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private var currentSessionId: Long = -1L

    private var isImageMode = false
    private var selectedImageUri: Uri? = null
    private var isVoiceModeActive = false
    private var isPickingProfileImage = false

    private lateinit var geminiApi: GeminiApi
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var currentBaseUrl: String = ""
    private var currentApiKey: String = ""
    private var currentModel: String = ""

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            if (isPickingProfileImage) {
                updateUserProfileImage(uri)
            } else {
                selectedImageUri = uri
                showImagePreview(uri)
                sendButton.visibility = View.VISIBLE
                micButton.visibility = View.GONE
            }
        }
        isPickingProfileImage = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)
        initBilling()
        MobileAds.initialize(this) {}
        loadRewardedAd()

        initViews()
        setupSidebar()
        refreshRemoteConfig()
        loadSuggestions()

        try {
            textToSpeech = TextToSpeech(this, this)
            initSpeechRecognizer()
        } catch (e: Exception) {}

        currentSessionId = intent.getLongExtra("SESSION_ID", -1L)
        setupRecyclerView()

        if (currentSessionId != -1L) {
            loadMessagesFromDb(currentSessionId)
        } else {
            addWelcomeMessage()
        }
        updateSidebarHistory()
    }

    override fun onResume() {
        super.onResume()
        updateSidebarHistory()
        updateCoinUI()
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) { textToSpeech.stop(); textToSpeech.shutdown() }
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        super.onDestroy()
    }

    private fun updateUserProfileImage(uri: Uri) {
        sidebarAvatarImage.visibility = View.VISIBLE
        sidebarAvatarText.visibility = View.GONE
        sidebarAvatarImage.load(uri) { transformations(CircleCropTransformation()) }
        sharedPreferences.edit().putString("PROFILE_URI", uri.toString()).apply()
        Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            sidebarUserEmail.text = user.email ?: "Free"
            val name = user.displayName ?: user.email?.substringBefore("@") ?: "User"
            sidebarUserName.text = name
            if (sidebarUserName.text.isNotEmpty()) sidebarAvatarText.text = sidebarUserName.text.first().toString().uppercase()

            if (user.photoUrl != null) {
                sidebarAvatarImage.visibility = View.VISIBLE
                sidebarAvatarText.visibility = View.GONE
                sidebarAvatarImage.load(user.photoUrl) { transformations(CircleCropTransformation()) }
            } else {
                val localUriString = sharedPreferences.getString("PROFILE_URI", null)
                if (localUriString != null) {
                    sidebarAvatarImage.visibility = View.VISIBLE
                    sidebarAvatarText.visibility = View.GONE
                    sidebarAvatarImage.load(Uri.parse(localUriString)) { transformations(CircleCropTransformation()) }
                }
            }
        }
    }

    private fun setupSidebar() {
        btnNewChat.setOnClickListener { startNewChat(); drawerLayout.closeDrawer(GravityCompat.START) }

        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", true)
        switchDarkMode.isChecked = isDarkMode

        fun toggleTheme(enableDark: Boolean) {
            sharedPreferences.edit().putBoolean("DARK_MODE", enableDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (enableDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        btnDarkMode.setOnClickListener {
            switchDarkMode.isChecked = !switchDarkMode.isChecked
            toggleTheme(switchDarkMode.isChecked)
        }
        switchDarkMode.setOnClickListener { toggleTheme(switchDarkMode.isChecked) }

        userProfileSection.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Account").setMessage("Log out?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null).show()
        }
        loadUserProfile()

        btnGetCoins = findViewById(R.id.btnGetCoins)
        btnGoPro = findViewById(R.id.btnGoPro)
        btnGetCoins.setOnClickListener { showAdForCoins() }
        btnGoPro.setOnClickListener { purchaseProSubscription() }
    }

    private fun initBilling() {
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) checkSubscriptionStatus()
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun checkSubscriptionStatus() {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { _, purchases ->
            isProUser = purchases.any { it.products.contains(SUBSCRIPTION_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            runOnUiThread { updateCoinUI() }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            isProUser = true
            updateCoinUI()
            Toast.makeText(this, "Pro Activated!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun purchaseProSubscription() {
        val productList = listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(SUBSCRIPTION_ID).setProductType(BillingClient.ProductType.SUBS).build())
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(productList).build()) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetailsList[0]).setOfferToken(productDetailsList[0].subscriptionOfferDetails?.get(0)?.offerToken ?: "").build())).build()
                billingClient.launchBillingFlow(this, flowParams)
            } else { runOnUiThread { Toast.makeText(this, "Store unavailable", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun loadRewardedAd() {
        RewardedAd.load(this, AD_UNIT_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        })
    }

    private fun showAdForCoins() {
        if (rewardedAd != null) {
            rewardedAd?.show(this) {
                addCoins(coinsRewardAd)
                loadRewardedAd()
                Toast.makeText(this, "+$coinsRewardAd Coins!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Ad loading...", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun getCoinBalance(): Int = sharedPreferences.getInt("COIN_BALANCE", 50)

    private fun addCoins(amount: Int) {
        sharedPreferences.edit().putInt("COIN_BALANCE", getCoinBalance() + amount).apply()
        updateCoinUI()
    }

    private fun deductCoins(amount: Int): Boolean {
        if (isProUser) return true
        val current = getCoinBalance()
        if (current >= amount) {
            sharedPreferences.edit().putInt("COIN_BALANCE", current - amount).apply()
            updateCoinUI()
            return true
        }
        return false
    }

    private fun updateCoinUI() {
        try {
            val balance = getCoinBalance()
            if (::toolbarCoinText.isInitialized) {
                if (isProUser) {
                    toolbarCoinChip.visibility = View.GONE
                    btnGoPro.visibility = View.GONE
                } else {
                    toolbarCoinChip.visibility = View.VISIBLE
                    toolbarCoinText.text = balance.toString()
                    btnGoPro.visibility = View.VISIBLE
                }
            }
            if (::sidebarCoins.isInitialized) {
                if (isProUser) {
                    sidebarCoins.text = "Pro Member"
                    sidebarCoins.setTextColor(Color.parseColor("#FFD700"))
                } else {
                    sidebarCoins.text = "Coins: $balance"
                    sidebarCoins.setTextColor(Color.WHITE)
                }
            }
        } catch (e: Exception) {}
    }

    private fun refreshRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build())
        val defaults = mapOf("admin_base_url" to "https://api.groq.com/openai/", "admin_api_key" to getApiKey(), "admin_model_name" to "llama-3.1-8b-instant", "economy_cost_image" to 5, "economy_reward_ad" to 30)
        remoteConfig.setDefaultsAsync(defaults)

        remoteConfig.fetchAndActivate().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                currentBaseUrl = remoteConfig.getString("admin_base_url")
                currentApiKey = remoteConfig.getString("admin_api_key")
                currentModel = remoteConfig.getString("admin_model_name")
                coinsPerImage = remoteConfig.getLong("economy_cost_image").toInt()
                coinsRewardAd = remoteConfig.getLong("economy_reward_ad").toInt()
            } else {
                currentBaseUrl = defaults["admin_base_url"] as String
                currentApiKey = defaults["admin_api_key"] as String
                currentModel = defaults["admin_model_name"] as String
            }
            try {
                geminiApi = Retrofit.Builder().baseUrl(currentBaseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(GeminiApi::class.java)
            } catch (e: Exception) {}
            runOnUiThread { updateCoinUI() }
        }
    }

    private fun checkAudioPermissionAndStartVoiceMode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            startVoiceMode()
        }
    }

    private fun startVoiceMode() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Voice n/a", Toast.LENGTH_SHORT).show()
            return
        }
        isVoiceModeActive = true
        voiceOverlay.visibility = View.VISIBLE
        voiceStatusText.text = "Listening..."
        startPulseAnimation()
        voiceOverlay.postDelayed({ startListening() }, 300)
    }

    private fun stopVoiceMode() {
        isVoiceModeActive = false
        voiceOverlay.visibility = View.GONE
        voicePulse.clearAnimation()
        speechRecognizer.stopListening()
        if (textToSpeech.isSpeaking) textToSpeech.stop()
    }

    private fun startListening() {
        if (!isVoiceModeActive) return
        runOnUiThread {
            voiceStatusText.text = "Listening..."
            voicePulse.alpha = 1.0f
            startPulseAnimation()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try { speechRecognizer.startListening(intent) } catch (e: Exception) { stopVoiceMode() }
    }

    private fun startPulseAnimation() {
        val anim = ScaleAnimation(1f, 1.2f, 1f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        anim.duration = 800
        anim.repeatCount = Animation.INFINITE
        anim.repeatMode = Animation.REVERSE
        voicePulse.startAnimation(anim)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { voiceStatusText.text = "Thinking..." }
                override fun onError(error: Int) {
                    if (isVoiceModeActive && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) startListening()
                    else if (isVoiceModeActive) voiceStatusText.text = "Try again..."
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        if (isVoiceModeActive) sendMessage(text) else userInput.setText(text)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) textToSpeech.setLanguage(Locale.US)
            isTtsReady = true
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runOnUiThread { if (isVoiceModeActive) { voiceStatusText.text = "Speaking..."; startPulseAnimation() } }
                }
                override fun onDone(utteranceId: String?) {
                    if (isVoiceModeActive) { runOnUiThread { voiceOverlay.postDelayed({ startListening() }, 500) } }
                }
                override fun onError(utteranceId: String?) {}
            })
        }
    }

    private fun showImagePreview(uri: Uri) {
        imagePreviewContainer.visibility = View.VISIBLE
        imagePreview.load(uri) {
            crossfade(true)
            transformations(RoundedCornersTransformation(16f))
            placeholder(R.drawable.ic_custom_add)
        }
    }

    private fun clearSelectedImage() {
        selectedImageUri = null
        imagePreviewContainer.visibility = View.GONE
        imagePreview.setImageDrawable(null)
        if (userInput.text.isNullOrEmpty()) {
            sendButton.visibility = View.GONE
            micButton.visibility = View.VISIBLE
        }
    }

    private fun processUserMessage(text: String) {
        if (isImageMode && selectedImageUri == null) {
            if (deductCoins(coinsPerImage)) {
                sendImageRequest(text)
            } else {
                showCoinDialog()
            }
        } else {
            sendMessage(text)
        }
    }

    private fun showCoinDialog() {
        AlertDialog.Builder(this)
            .setTitle("Not Enough Coins! 🪙")
            .setMessage("Cost: $coinsPerImage coins.\nGet $coinsRewardAd free coins?")
            .setPositiveButton("Watch Ad") { _, _ -> showAdForCoins() }
            .setNegativeButton("Go Pro") { _, _ -> purchaseProSubscription() }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun sendImageRequest(prompt: String) {
        suggestionsRecyclerView.visibility = View.GONE
        messageList.add(Message("Generating: $prompt", Message.SENT_BY_USER))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)
        userInput.setText("")
        clearSelectedImage()

        val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
        val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt"
        recyclerView.postDelayed({ addBotImage(imageUrl) }, 500)
    }

    private fun addBotImage(url: String) {
        runOnUiThread {
            messageList.add(Message("", Message.SENT_BY_BOT, imageUrl = url))
            chatAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.scrollToPosition(messageList.size - 1)
            lifecycleScope.launch { db.chatDao().insertMessage(ChatMessage(sessionId = currentSessionId, content = "Image: $url", isUser = false)) }
        }
    }

    private fun sendMessage(userText: String) {
        suggestionsRecyclerView.visibility = View.GONE
        val finalText = if (userText.isEmpty() && selectedImageUri != null) "Analyze this image" else userText
        val currentImageUri = selectedImageUri

        runOnUiThread {
            messageList.add(Message(finalText, Message.SENT_BY_USER, imageUrl = currentImageUri?.toString()))
            chatAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.scrollToPosition(messageList.size - 1)
            userInput.setText("")
            clearSelectedImage()
        }

        lifecycleScope.launch {
            ensureSessionExists(finalText)
            db.chatDao().insertMessage(ChatMessage(sessionId = currentSessionId, content = finalText, isUser = true, localImageUri = currentImageUri?.toString()))
            db.chatDao().updateSessionTime(currentSessionId, System.currentTimeMillis())

            // 🛠️ FIX: Update Sidebar on IO then Main
            withContext(Dispatchers.Main) { updateSidebarHistory() }

            val apiContent: Any = if (currentImageUri != null) {
                val base64Image = uriToBase64(currentImageUri)
                if (base64Image != null) createImageContent(finalText, base64Image) else createTextContent(finalText + " [Image Error]")
            } else {
                createTextContent(finalText)
            }

            val apiMessages = listOf(GroqMessage(role = "user", content = apiContent))
            val request = GroqRequest(model = currentModel, messages = apiMessages)
            val authHeader = "Bearer $currentApiKey"

            geminiApi.getResponse(authHeader, request).enqueue(object : Callback<GroqResponse> {
                override fun onResponse(call: Call<GroqResponse>, response: Response<GroqResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val choices = response.body()!!.choices
                        if (choices.isNotEmpty()) {
                            val reply = choices[0].message.content.toString()
                            addBotResponse(reply)
                            if (isTtsReady && isVoiceModeActive) {
                                val params = Bundle()
                                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AI_RESPONSE")
                                textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, params, "AI_RESPONSE")
                            }
                        }
                    } else {
                        addBotResponse("Error ${response.code()}: ${response.message()}")
                    }
                }
                override fun onFailure(call: Call<GroqResponse>, t: Throwable) {
                    addBotResponse("Network Fail: ${t.message}")
                }
            })
        }
    }

    private suspend fun uriToBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArrays = outputStream.toByteArray()
            Base64.encodeToString(byteArrays, Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    private fun addBotResponse(text: String) {
        runOnUiThread {
            messageList.add(Message(text, Message.SENT_BY_BOT))
            chatAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.scrollToPosition(messageList.size - 1)
            lifecycleScope.launch {
                if (currentSessionId != -1L) db.chatDao().insertMessage(ChatMessage(sessionId = currentSessionId, content = text, isUser = false))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAudioPermissionAndStartVoiceMode()
        }
    }

    private fun loadSuggestions() {
        try {
            val json = assets.open("prompts.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<Prompt>>() {}.type
            val prompts: List<Prompt> = Gson().fromJson(json, listType)
            suggestionsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            suggestionsRecyclerView.adapter = SuggestionAdapter(prompts) { promptText -> processUserMessage(promptText) }
        } catch (e: Exception) {}
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.menuButton)
        btnTopNewChat = findViewById(R.id.btnTopNewChat)
        toolbarCoinChip = findViewById(R.id.toolbarCoinChip)
        toolbarCoinText = findViewById(R.id.toolbarCoinText)
        toolbarCoinChip.setOnClickListener { showCoinDialog() }

        recyclerView = findViewById(R.id.recyclerView)
        suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView)
        userInput = findViewById(R.id.userInput)
        sendButton = findViewById(R.id.sendButton)
        micButton = findViewById(R.id.micButton)
        btnImageMode = findViewById(R.id.btnImageMode)
        btnAttachImage = findViewById(R.id.btnAttachImage)
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer)
        imagePreview = findViewById(R.id.imagePreview)
        btnClosePreview = findViewById(R.id.btnClosePreview)
        voiceOverlay = findViewById(R.id.voiceOverlay)
        voiceStatusText = findViewById(R.id.voiceStatusText)
        btnStopVoice = findViewById(R.id.btnStopVoice)
        voicePulse = findViewById(R.id.voicePulse)

        sidebarRecyclerView = findViewById(R.id.historyContainer)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnDarkMode = findViewById(R.id.btnDarkMode)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        userProfileSection = findViewById(R.id.userProfileSection)
        sidebarUserName = findViewById(R.id.sidebarUserName)
        sidebarUserEmail = findViewById(R.id.sidebarUserEmail)
        sidebarAvatarText = findViewById(R.id.sidebarAvatarText)
        sidebarAvatarImage = findViewById(R.id.sidebarAvatarImage)
        sidebarAvatarContainer = findViewById(R.id.sidebarAvatarContainer)

        try { sidebarCoins = findViewById(R.id.sidebarCoins) } catch (e: Exception) {}

        sidebarAvatarContainer.setOnClickListener { isPickingProfileImage = true; pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        sidebarRecyclerView.layoutManager = LinearLayoutManager(this)
        menuButton.setOnClickListener { if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START) else drawerLayout.openDrawer(GravityCompat.START) }
        btnTopNewChat.setOnClickListener { startNewChat() }

        userInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { if (s.isNullOrEmpty() && selectedImageUri == null) { sendButton.visibility = View.GONE; micButton.visibility = View.VISIBLE } else { sendButton.visibility = View.VISIBLE; micButton.visibility = View.GONE } }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnImageMode.setOnClickListener {
            isImageMode = !isImageMode
            if (isImageMode) { userInput.hint = "Gen Image ($coinsPerImage Coins)..."; btnImageMode.setColorFilter(Color.YELLOW) }
            else { userInput.hint = "Message..."; btnImageMode.setColorFilter(Color.parseColor("#8B5CF6")) }
        }

        btnAttachImage.setOnClickListener { isPickingProfileImage = false; pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        btnClosePreview.setOnClickListener { clearSelectedImage() }
        sendButton.setOnClickListener { val text = userInput.text.toString().trim(); if (text.isNotEmpty() || selectedImageUri != null) processUserMessage(text) }
        micButton.setOnClickListener { checkAudioPermissionAndStartVoiceMode() }
        btnStopVoice.setOnClickListener { stopVoiceMode() }

        updateCoinUI()
    }

    private fun startNewChat() {
        currentSessionId = -1L
        val size = messageList.size
        messageList.clear()
        chatAdapter.notifyItemRangeRemoved(0, size)
        addWelcomeMessage()
    }

    private fun addWelcomeMessage() {
        if (messageList.isEmpty()) {
            messageList.add(Message("Hello! I am ConvoAI.", Message.SENT_BY_BOT))
            chatAdapter.notifyItemInserted(0)
            suggestionsRecyclerView.visibility = View.VISIBLE
        }
    }

    // 🛠️ FIX: Update Sidebar safely
    private fun updateSidebarHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sessions = db.chatDao().getAllSessions()
            withContext(Dispatchers.Main) {
                if (::sidebarAdapter.isInitialized) {
                    sidebarAdapter.updateData(sessions)
                } else {
                    sidebarAdapter = SidebarAdapter(sessions,
                        { session ->
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(userInput.windowToken, 0)
                            currentSessionId = session.id
                            loadMessagesFromDb(currentSessionId)
                            drawerLayout.closeDrawer(GravityCompat.START)
                        },
                        { session -> showRenameDialog(session) },
                        { session -> deleteChat(session) },
                        { session -> shareChat(session) }
                    )
                    sidebarRecyclerView.adapter = sidebarAdapter
                }
            }
        }
    }

    // 🛠️ CRITICAL FIX: Safe Rename Logic
    private fun showRenameDialog(session: ChatSession) {
        val input = EditText(this)
        input.setText(session.title)

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(60, 20, 60, 20)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // This now works because DAO has OnConflictStrategy.REPLACE
                            db.chatDao().insertSession(session.copy(title = newTitle))
                            withContext(Dispatchers.Main) {
                                updateSidebarHistory()
                                Toast.makeText(this@MainActivity, "Chat renamed", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Rename failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 🛠️ FIX: Safe Delete Logic
    private fun deleteChat(session: ChatSession) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.chatDao().deleteSession(session)
            withContext(Dispatchers.Main) {
                updateSidebarHistory()
                if (currentSessionId == session.id) startNewChat()
            }
        }
    }

    private fun shareChat(session: ChatSession) {
        lifecycleScope.launch {
            val messages = db.chatDao().getMessagesForSession(session.id)
            val shareText = StringBuilder("Chat: ${session.title}\n\n")
            messages.forEach { shareText.append("${if(it.isUser) "You" else "AI"}: ${it.content}\n") }
            startActivity(Intent.createChooser(Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText.toString()); type = "text/plain" }, "Share Chat via"))
        }
    }

    private fun setupRecyclerView() {
        messageList = mutableListOf()
        chatAdapter = ChatAdapter(messageList)
        recyclerView.adapter = chatAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private suspend fun ensureSessionExists(firstMessage: String) {
        if (currentSessionId == -1L) {
            val title = if (firstMessage.length > 30) firstMessage.take(30) + "..." else firstMessage
            currentSessionId = db.chatDao().insertSession(ChatSession(title = title))
        }
    }

    private fun loadMessagesFromDb(sessionId: Long) {
        lifecycleScope.launch {
            val savedMessages = db.chatDao().getMessagesForSession(sessionId)
            val oldSize = messageList.size
            messageList.clear()
            chatAdapter.notifyItemRangeRemoved(0, oldSize)
            for (msg in savedMessages) {
                val imageUrl = if (msg.content.startsWith("Image: https")) msg.content.substring(7) else msg.localImageUri
                val type = if (msg.isUser) Message.SENT_BY_USER else Message.SENT_BY_BOT
                val contentToShow = if(msg.isUser && imageUrl != null) msg.content else if (imageUrl != null) "" else msg.content
                messageList.add(Message(contentToShow, type, imageUrl = imageUrl))
            }
            chatAdapter.notifyItemRangeInserted(0, messageList.size)
            suggestionsRecyclerView.visibility = View.GONE
            if (messageList.isNotEmpty()) recyclerView.scrollToPosition(messageList.size - 1)
        }
    }
}