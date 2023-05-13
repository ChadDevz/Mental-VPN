package com.chadx.v2ray.ph.ui

// import com.chadx.v2ray.ph.AppConfig.ANG_PACKAGE

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.*
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tbruyelle.rxpermissions.RxPermissions
import com.tencent.mmkv.MMKV
import com.chadx.v2ray.ph.AppConfig
import com.chadx.v2ray.ph.BuildConfig
import com.chadx.v2ray.ph.R
import com.chadx.v2ray.ph.adapter.CronoksAdapter
import com.chadx.v2ray.ph.antipiracy.AntiPiracyActivity
import com.chadx.v2ray.ph.databinding.ActivityMainBinding
import com.chadx.v2ray.ph.dto.EConfigType
import com.chadx.v2ray.ph.extension.toSpeedString
import com.chadx.v2ray.ph.extension.toast
import com.chadx.v2ray.ph.helper.SimpleItemTouchHelperCallback
import com.chadx.v2ray.ph.service.V2RayServiceManager
import com.chadx.v2ray.ph.service.V2RayServiceManager.startV2Ray
import com.chadx.v2ray.ph.util.AngConfigManager
import com.chadx.v2ray.ph.util.Crypt
import com.chadx.v2ray.ph.util.MmkvManager
import com.chadx.v2ray.ph.util.Utils
import com.chadx.v2ray.ph.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.drakeet.support.toast.ToastCompat
import org.json.JSONArray
import org.json.JSONObject
import rx.Observable
import rx.Subscription
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random


class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    private val adapter by lazy { MainRecyclerAdapter(this) }
    private val mainStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }
    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startV2Ray()
            // for premium method
                /* val usernameval= findViewById<EditText>(R.id.et_username) as EditText
                val passwordval = findViewById<EditText>(R.id.et_password) as EditText
                if (usernameval.text.toString().isEmpty() && passwordval.text.toString().isEmpty()){
                    toast("Please Fill Up the username And Password")
                }else {
                    startV2Ray()
                    val preferenceuseres=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                    val editoruseres=preferenceuseres.edit()
                    editoruseres.putString("username", usernameval.text.toString())
                    editoruseres.commit()

                    val preferencepass=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                    val editorpass=preferencepass.edit()
                    editorpass.putString("password", passwordval.text.toString())
                    editorpass.commit()
                    bootmeup()
            }*/
        }
    }
    private var mItemTouchHelper: ItemTouchHelper? = null
    val mainViewModel: MainViewModel by viewModels()


    var v2ray: String? = null

    var jsonString = ""
    lateinit var textview: TextView
    lateinit var serverspin: Spinner

    var usernameval =""
    var passwordval = ""
    var errorrr = ""
    var credddetails = ""
    var validatestring = ""
    var conditionstring = ""
    private var mTimeLeftInMillis: Long = 0
    private var mCountDownTimer: CountDownTimer? = null
    private var mTimerRunning = false
    private var saved_ads_time: Long = 0
    private var mEndTime: Long = 0
    private var mTimerEnabled = false
    private var mBtnCountDown: CountDownTimer? = null
    private var mTimeLeftBtn: Long = 0
    lateinit var buttonSet: Button
    lateinit var mAdView: AdView
    lateinit var mDialog: ProgressDialog
    lateinit var mDialog1: ProgressDialog
    var mRewardedAd: RewardedAd? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var TAG = "Intertitial"
    private var isConnected = false
    var notificationconfigname = ""
    private var lastQueryTime = 0L
    private var mSubscription: Subscription? = null

    @SuppressLint("ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        title = getString(R.string.title_server)
        setSupportActionBar(binding.toolbar)
        autoupdate()


        MobileAds.initialize(this) {

        }

        if (verifyInstallerId(this)) {
            // do nothing
        } else {
            if (BuildConfig.DEBUG) {
            } else {
                val antipiracy = Intent(this, AntiPiracyActivity::class.java)
                startActivity(antipiracy)
                finish()
            }
        }
        binding.appver.text = BuildConfig.VERSION_NAME

        val isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true)

        if (isFirstRun) {

            // add free time
            val ads_time: Long = (1 * 1 * 3600 * 1000).toLong()
            addTime(ads_time)
            saveTime()
            // resumeTime()


            // grab server from assets
               jsonString = BuildConfig.BRIEFCASE

               val jsonObject = JSONObject(Crypt.parseToString(jsonString))
               val jsonarray = jsonObject.getJSONArray("Servers");

               val jsonversion = jsonObject.getString("Version")
            // val jsonrelrasedfh = jsonObject.getString("ReleaseNotes")

            // server version & release notes
            serverSPIN(jsonarray.toString(), jsonversion.toString())

            // for premium method
            /* bootmeup()
            getdevicecred() */

            val preference=getSharedPreferences(resources.getString(R.string.app_name), MODE_PRIVATE)
            val editor=preference.edit()
            editor.putString("json", jsonarray.toString())
            editor.commit()

            // server version & release notes
            val preference1=getSharedPreferences(resources.getString(R.string.app_name), MODE_PRIVATE)
            val editor1=preference1.edit()
            editor1.putString("configrelease", jsonversion)
            editor1.commit()
        }else{
            resumeTime()
            // for premium method
            /* bootmeup()
            getdevicecred() */

            val usernameval1 = findViewById<EditText>(R.id.et_username) as EditText
            val passwordval1 = findViewById<EditText>(R.id.et_password) as EditText

            // grab username
            val preferenceuser = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
            val userrrrr = preferenceuser.getString("username", "")
            usernameval1.setText(userrrrr.toString())
            Log.d("lol", userrrrr.toString())

            val preferencepass = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
            val passsss = preferencepass.getString("password", "")
            passwordval1.setText(passsss.toString())



            // grab server from assets
            /* jsonString = BuildConfig.BRIEFCASE

            val jsonObject = JSONObject(Crypt.parseToString(jsonString))
            val jsonarray = jsonObject.getJSONArray("Servers");

            val jsonversion = jsonObject.getString("Version") */
            // val jsonrelrasedfh = jsonObject.getString("ReleaseNotes")

            // get json from stored data
            val preference1=getSharedPreferences(resources.getString(R.string.app_name), MODE_PRIVATE)
            val jsonconfig= preference1.getString("json", "")

            // get version from stored data
            val preference=getSharedPreferences(resources.getString(R.string.app_name), MODE_PRIVATE)
            val jsonconfigversion= preference.getString("configrelease", "")

            // server version & release notes

            serverSPIN(jsonconfig.toString(), jsonconfigversion.toString())
        }

        buttonSet = findViewById<Button>(R.id.btnAddTime)

        buttonSet.setOnClickListener(View.OnClickListener {
            if (isConnected){
               loadRewardedVideoAd()
            }else {
                toast("Please Connect VPN First")
            }

        })

        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                .putBoolean("isFirstRun", false).apply()

        binding.fab.setOnClickListener {
            val usernameval = findViewById<EditText>(R.id.et_username) as EditText
            val passwordval = findViewById<EditText>(R.id.et_password) as EditText
            if (mainViewModel.isRunning.value == true) {
                Utils.stopVService(this)
            } else if (settingsStorage?.decodeString(AppConfig.PREF_MODE) ?: "VPN" == "VPN") {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startV2Ray()

                    // for premium method
                    /* if (usernameval.text.toString().isEmpty() && passwordval.text.toString().isEmpty()){
                        toast("Please Fill Up the username And Password")
                    }else {
                        startV2Ray()
                        val preferenceuseres=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                        val editoruseres=preferenceuseres.edit()
                        editoruseres.putString("username", usernameval.text.toString())
                        editoruseres.commit()

                        val preferencepass=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                        val editorpass=preferencepass.edit()
                        editorpass.putString("password", passwordval.text.toString())
                        editorpass.commit()
                        bootmeup()
                    }*/

                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                startV2Ray()
                // for premium method
                /* if (usernameval.text.toString().isEmpty() && passwordval.text.toString().isEmpty()){
                    toast("Please Fill Up the username And Password")
                }else {
                    startV2Ray()
                    val preferenceuseres=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                    val editoruseres=preferenceuseres.edit()
                    editoruseres.putString("username", usernameval.text.toString())
                    editoruseres.commit()

                    val preferencepass=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                    val editorpass=preferencepass.edit()
                    editorpass.putString("password", passwordval.text.toString())
                    editorpass.commit()
                    bootmeup()
                } */
            }
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.tvTestState.setOnClickListener(View.OnClickListener {

        if (mainViewModel.isRunning.value == true) {
            binding.tvTestState.text = getString(R.string.connection_test_testing)
            mainViewModel.testCurrentServerRealPing()
        } else {
//                tv_test_state.text = getString(R.string.connection_test_fail)
        }
        })

        val callback = SimpleItemTouchHelperCallback(adapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(binding.recyclerView)

        setupViewModelObserver()
        migrateLegacy()

        val bottomNavigationView =
            findViewById<View>(R.id.bottom_navigation) as BottomNavigationView

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_update -> {
                    /* val myRunnable = MainActivity.Conn(mHandler)
                    val myThread = Thread(myRunnable)
                    myThread.start() */
                    conditionstring = "true"
                    toast("Update Feature Not Available at this moment wait contact the developer for updates")
                }
                /* R.id.bottom_device_info -> {
                    val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val inflate: View =
                        inflater.inflate(R.layout.activity_device_idactivity, null as ViewGroup?)
                    val builer = AlertDialog.Builder(this)
                    builer.setView(inflate)
                    val deviceid = inflate.findViewById<View>(R.id.tv_deviceid) as TextView
                    val devicemodel = inflate.findViewById<View>(R.id.tv_devicemodel) as TextView
                    val copyid = inflate.findViewById<View>(R.id.btn_copy) as Button


                    // grab device id and device model
                    val preferencedeviceid = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                    deviceid.text = preferencedeviceid.getString("deviceid", "")

                    val preferencedevicemodel = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
                    devicemodel.text = preferencedevicemodel.getString("devicemodel", "")


                    val alert = builer.create()
                    alert.setCancelable(true)
                    alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    alert.window!!.setGravity(Gravity.CENTER)
                    try {
                        alert.show()
                    } catch (e: java.lang.Exception) {
                        // WindowManager$BadTokenException will be caught and the app would not display
                        // the 'Force Close' message
                    }
                    copyid.setOnClickListener {

                        val talktalk =
                            "Mediatek Client Device Info \n"+
                            "Device ID: "+preferencedeviceid.getString("deviceid", "")+"\n"+
                            "Device Model: "+preferencedevicemodel.getString("devicemodel", "")
                        val clipboard: ClipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("", talktalk)
                        clipboard.setPrimaryClip(clip)
                        toast("device info copied")
                        alert.dismiss()

                    }
                }*/
                R.id.bottom_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.bottom_exit -> {

                    val dialogBuilder = AlertDialog.Builder(this)

                    // set message of alert dialog
                    dialogBuilder.setMessage("Do you want to close this application ?")
                        // if the dialog is cancelable
                        .setCancelable(false)
                        // positive button text and action
                        .setPositiveButton("Proceed", DialogInterface.OnClickListener {
                                dialog, id -> finish()
                        })
                        // negative button text and action
                        .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                                dialog, id -> dialog.cancel()
                        })

                    // create dialog box
                    val alert = dialogBuilder.create()
                    // set title for alert dialog box
                    alert.setTitle("Caution !")
                    // show alert dialog
                    alert.show()
                }
            }
            false
        }
    }

    private fun connecting() {
        mDialog1 = ProgressDialog(this)
        mDialog1.setTitle("Connecting")
        mDialog1.setMessage("Confirming Connection please wait...")
        mDialog1.setCancelable(true)
        mDialog1.show()
    }

    private fun loadingAds() {
        mDialog = ProgressDialog(this)
        mDialog.setTitle("Loading Rewarded Ad")
        mDialog.setMessage("Please wait while loading... \n\nNote: \nYou need to finish the video to claim your time reward")
        mDialog.setCancelable(true)
        mDialog.show()
    }

    private fun showError() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error Loading Ad")
            .setMessage("Failed to load Ad, please check your internet connection !! \n\nNote: If this error still continue please contact the developer for further assistance.")
            .show()
    }

    private fun loadRewardedVideoAd() {
        loadingAds()
        loadRewardedAd()
    }

    fun verifyInstallerId(context: Context): Boolean {
        // A list with valid installers package name
        val validInstallers: List<String> =
            ArrayList(Arrays.asList("com.android.vending", "com.google.android.feedback"))

        // The package name of the app that has installed your app
        val installer = context.packageManager.getInstallerPackageName(context.packageName)

        // true if your app has been downloaded from Play Store
        return installer != null && validInstallers.contains(installer)
    }

    private fun loadRewardedAd() {

        // load rewarded ad
        var adRequest1 = AdRequest.Builder().build()

        RewardedAd.load(this,"ca-app-pub-3112752528168482/1775068715", adRequest1, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })

        mRewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                /* Handler().postDelayed({
                    addtimemethod()
                }, 8000) */
                mDialog.dismiss()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                mRewardedAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                mRewardedAd = null
            }
        }
        if (mRewardedAd != null) {
            mRewardedAd?.show(this, OnUserEarnedRewardListener {
                fun onUserEarnedReward(rewardItem: RewardItem) {
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                       addtimemethod()
                }
                onUserEarnedReward(it)

            })
        } else {
            Log.d("onrewarded", "The rewarded ad wasn't ready yet.")
        }
    }

    private fun Loadintertitial() {
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3112752528168482/5452084107", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
                showAd()
            }
        })

        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
                mDialog1.dismiss()
            }
        }
    }

    private fun showAd() {
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(this)
            } else {
                Log.d(TAG, "The interstitial ad wasn't ready yet.")
            }
    }

    private fun start() {
        if (saved_ads_time.equals(0)) {
            Toast.makeText(
                this@MainActivity,
                "Your time is expiring soon, please click ADD TIME to renew access!",
                Toast.LENGTH_LONG
            ).show()
            val millisInput = (3600 * 1000).toLong()
            setTime(millisInput)
        }
        if (!mTimerRunning) {
            startTimer()
        }
    }


    private fun stop() {
        if (mTimerRunning) {
            pauseTimer()
        }
    }

    private fun addTime(time: Long) {
        setTime(time)
        if (mTimerRunning) {
            pauseTimer()

        }
        startTimer()
    }

    private fun delaysleep(){
        Handler().postDelayed({
            startTimer()
        }, 1000)
    }

    private fun pauseTimer() {
        mCountDownTimer?.cancel()
        mTimerRunning = false
    }

    fun addtimemethod() {
        val ads_time: Long = (2 * 1 * 3600 * 1000).toLong()
        addTime(ads_time)
        toast("2 hours has been succesfully added")
        mDialog.dismiss()
    }

    private fun updateCountDownText() {
        val mTextViewCountDown = findViewById<TextView>(R.id.tvTimeRemaining)
        val days: Long = TimeUnit.MILLISECONDS.toDays(mTimeLeftInMillis)
        val daysMillis: Long = TimeUnit.DAYS.toMillis(days)
        val hours: Long = TimeUnit.MILLISECONDS.toHours(mTimeLeftInMillis - daysMillis)
        val hoursMillis: Long = TimeUnit.HOURS.toMillis(hours)
        val minutes: Long =
            TimeUnit.MILLISECONDS.toMinutes(mTimeLeftInMillis - daysMillis - hoursMillis)
        val minutesMillis: Long = TimeUnit.MINUTES.toMillis(minutes)
        val seconds: Long =
            TimeUnit.MILLISECONDS.toSeconds(mTimeLeftInMillis - daysMillis - hoursMillis - minutesMillis)
        val resultString = days.toString() + "d:" + hours + "h:" + minutes + "m:" + seconds + "s"

        // VIEW TIMER HERE
        mTextViewCountDown.setText(resultString)
    }

    private fun setTime(milliseconds: Long) {
        saved_ads_time = mTimeLeftInMillis + milliseconds
        mTimeLeftInMillis = saved_ads_time
        updateCountDownText()
    }

    private fun saveTime() {
        val saved_current_time = getSharedPreferences("time", MODE_PRIVATE)
        val time_edit = saved_current_time.edit()
        time_edit.putLong("SAVED_TIME", mTimeLeftInMillis)
        time_edit.apply()
    }

    private fun resumeTime() {
        val time = getSharedPreferences("time", MODE_PRIVATE)
        val saved_time = time.getLong("SAVED_TIME", 0)
        setTime(saved_time)

        // Use this code to continue time if app close accidentally while connected
        mTimerEnabled = true
    }

    private fun startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis
        mCountDownTimer = object : CountDownTimer(mTimeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                mTimeLeftInMillis = millisUntilFinished
                saveTime()
                updateCountDownText()
            }

            override fun onFinish() {
                mTimerRunning = false
                pauseTimer()
                saved_ads_time = 0
                // Code for auto stop vpn (v2ray)
                ifvalidatingistolong()
                Toast.makeText(
                    this@MainActivity,
                    "Time expired! Click Add + Time to renew access!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
        mTimerRunning = true
    }

    private fun btnTimer() {
        mBtnCountDown = object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                mTimeLeftBtn = millisUntilFinished
                buttonSet.visibility = View.GONE
                updateBtnText()
            }

            override fun onFinish() {
                buttonSet.visibility = View.VISIBLE
                buttonSet.setText("ADD + TIME")
            }
        }.start()
    }

    private fun updateBtnText() {
        val seconds = (mTimeLeftBtn / 1000) as Int % 60
        val timeLeftFormatted: String
        if (seconds > 0) {
            timeLeftFormatted = java.lang.String.format(
                Locale.getDefault(),
                "%02d", seconds
            )
            // buttonSet.setText("Refresh in $timeLeftFormatted")
        }
    }

    private fun autoupdate() {
        val myRunnable = MainActivity.Conn(mHandler)
        val myThread = Thread(myRunnable)
        myThread.start()
        conditionstring = "false"
    }

    private fun getdevicecred() {

        val number = Random.nextInt(99999999)

        // save device id and device model in preference
        val preferenceuseres=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editoruseres=preferenceuseres.edit()
        editoruseres.putString("deviceid", number.toString())
        editoruseres.commit()

        val preferencepass=getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editorpass=preferencepass.edit()
        editorpass.putString("devicemodel", Build.MODEL)
        editorpass.commit()
    }

    fun serverSPIN(jsonString: String, toString: String) {
        serverspin = findViewById<Spinner>(R.id.popeye)

        serverspin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, pos: Int, l: Long) {
                val country = serverspin.getItemAtPosition(pos).toString()

                val jsonArray1 = JSONArray(jsonString)
                val cname = jsonArray1.getJSONObject(pos).getString("Name")
                val cflag = jsonArray1.getJSONObject(pos).getString("Flag")
                val cv2ray = jsonArray1.getJSONObject(pos).getString("v2ray")

                // send this name into the notification
                notificationconfigname = cname
                // save data here
                try {
                    val guid = mainViewModel.serverList.getOrNull(0)
                        mainViewModel.removeServer(guid.toString())
                    importBatchConfig(cv2ray)
                }catch (e: Exception){
                    toast(e.toString())
                }
                }
                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    // DO Nothing here
                }
        }

        val ServerNames: java.util.ArrayList<JSONObject> = ArrayList()

        if (jsonString.isNullOrEmpty()){

        }else{
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()){
                ServerNames.add(jsonArray.getJSONObject(i))
                serverspin.adapter = CronoksAdapter(this@MainActivity, R.layout.spinner_item, ServerNames)

            }
        }

        if (toString.isNullOrEmpty()){
            // jsonparse()
        }else{
            binding.configver.text = toString
        }
    }

    private fun setupViewModelObserver() {
        mainViewModel.updateListAction.observe(this) {
            val index = it ?: return@observe
            if (index >= 0) {
                adapter.notifyItemChanged(index)
            } else {
                adapter.notifyDataSetChanged()
            }
        }
        mainViewModel.updateTestResultAction.observe(this) { binding.tvTestState.text = it }
        mainViewModel.isRunning.observe(this) {
            val isRunning = it ?: return@observe
            adapter.isRunning = isRunning
            if (isRunning) {
                binding.fab.text = "Disconnect"
                binding.tvTestState.text = getString(R.string.connection_connected)

                // load intertitial ad
                Loadintertitial()

                // load banner ad
                mAdView = findViewById(R.id.adView)
                val adRequest = AdRequest.Builder().build()
                mAdView.loadAd(adRequest)

                mAdView.adListener = object: AdListener() {
                    override fun onAdLoaded() {
                        // Code to be executed when an ad finishes loading.
                    }

                    override fun onAdFailedToLoad(adError : LoadAdError) {
                        // Code to be executed when an ad request fails.
                    }

                    override fun onAdOpened() {
                        // Code to be executed when an ad opens an overlay that
                        // covers the screen.
                    }

                    override fun onAdClicked() {
                        // Code to be executed when the user clicks on an ad.
                    }

                    override fun onAdClosed() {
                        // Code to be executed when the user is about to return
                        // to the app after tapping on an ad.
                    }
                }



                Snackbar.make(binding.root, ""+getString(R.string.connected), Snackbar.LENGTH_SHORT).show()
                start()

                // for premium method
                /*heartbeatmethod()
                bootmeup()*/

                isConnected = true
                connecting()
                binding.popeye.isEnabled = false
                binding.etUsername.isEnabled = false
                binding.etPassword.isEnabled = false
                netspeed()
            } else {

                isConnected = false
                binding.fab.text = getString(R.string.connection_not_connected)
                binding.tvTestState.text = "Disconnected"
                stop()
                binding.popeye.isEnabled = true
                binding.etUsername.isEnabled = true
                binding.etPassword.isEnabled = true
            }
        }
        mainViewModel.startListenBroadcast()
    }

    private val mHandler1: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(inputMessage: Message) { if (inputMessage.what == 0) {

            Log.d("cred1", inputMessage.obj.toString())
            credddetails = inputMessage.obj.toString()
            if(credddetails.contains("<!doctype html>")){
                toast("Something Went Wrong please Report this to The Administrator")
                nokstop()
            }else {
                // textview.text = "Obtaining Account Status"
                val jsonArray1 = JSONObject(inputMessage.obj.toString())
                val cname = jsonArray1.getBoolean("auth")
                // Log.d("crispy", credddetails)
                if (cname){
                    approve(credddetails)
                    devicematched(credddetails)
                }else{
                    denied()
                }
                upme()
            }
        }

        }
    }

    private fun heartbeatmethod() {
        val preferencedeviceid = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
        val deviceid = preferencedeviceid.getString("deviceid", "")

        val preferencedevicemodel = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
        val devidemodel = preferencedevicemodel.getString("devicemodel", "")
            Handler().postDelayed({
                val myRunnable = Conn1(mHandler1, usernameval, passwordval, deviceid.toString(), devidemodel.toString())
                val myThread = Thread(myRunnable)
                myThread.start()

            }, 2000)
    }

    private fun ifvalidatingistolong() {
        // textview.text = "Validating Take to Long Please Try Again"
        Utils.stopVService(this)
        mDialog1.dismiss()

    }

  class Conn1(
      mHand: Handler,
      val usernameval: String,
      val passwordval: String,
      val deviceid: String,
      val devidemodel: String
  ): Runnable {
        val myHandler1 = mHand
        override fun run() {
            var content = StringBuilder()
            try {

                // declare URL to text file, create a connection to it and put into stream.
                val myUrl = URL(Crypt.parseToString(BuildConfig.V2RAYENGINE)+"?username="+usernameval+"&password="+passwordval+"&device_id="+deviceid+"&device_model="+devidemodel)  // or URL to txt file
                // val myUrl = URL("https://google.com")  // this is a sample if api is not wot working
                val urlConnection = myUrl.openConnection() as HttpURLConnection
                val inputStream = urlConnection.inputStream

                // get text from stream, convert to string and send to main thread.
                val allText = inputStream.bufferedReader().use { it.readText() }
                content.append(allText)
                val str = content.toString()
                val msg = myHandler1.obtainMessage()
                msg.what = 0
                msg.obj = str
                myHandler1.sendMessage(msg)

            } catch (e: Exception) {
                Log.d("Error", e.toString())
                // errorrr = "Reaching Database Error Validating Account Failed... "
            }
        }
    }

    private fun approve(credddetails: String) {
        val strr0 = "Account confirmed ! for user: "+usernameval+" "+"Enjoy Browsing ! :D"
        // textview.text = strr0
        Snackbar.make(binding.root, strr0, Snackbar.LENGTH_SHORT).show()
        val jsonArray1 = JSONObject(credddetails)
        val accexp = jsonArray1.getString("expiry")
        binding.accountexp.text = accexp
        binding.popeye.isEnabled = false
        binding.etUsername.isEnabled = false
        binding.etPassword.isEnabled = false

    }

    private fun devicematched(credddetails: String) {
        val jsonArray1 = JSONObject(credddetails)
        val cname1 = jsonArray1.getString("device_match")

        if (cname1.equals("true")){
            // do nothing
        }else{
            Snackbar.make(binding.root, "this Account is used in other device", Snackbar.LENGTH_SHORT).show()
            Utils.stopVService(this)
            binding.popeye.isEnabled = true
            binding.etUsername.isEnabled = true
            binding.etPassword.isEnabled = true
        }

    }

    private fun denied() {
        val strr = "Authentication Problem for: " + usernameval + " error number: "+credddetails
        // textview.text = strr
        Utils.stopVService(this)
        Snackbar.make(binding.root, strr, Snackbar.LENGTH_SHORT).show()
        binding.popeye.isEnabled = true
        binding.etUsername.isEnabled = true
        binding.etPassword.isEnabled = true
    }

    private fun nokstop() {
        Utils.stopVService(this)
    }


    private fun systemerrorr() {
        // textview.text = "acc database crashed while connecting to database please try again"
        Utils.stopVService(this)
    }

    private fun upme() {
        Handler().postDelayed({
            // validatestring = textview.text.toString()
            if (validatestring.contains("...")) {
                ifvalidatingistolong()
                toast("toastchamp")
            } else {
                // nothing
            }
        }, 30000)
    }

    fun bootmeup(){
        // grab username
        val preferenceuser = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
        val userrrrr = preferenceuser.getString("username", "")
        val userrrname = userrrrr.toString()

        usernameval = userrrname

        val preferencepass = getSharedPreferences(resources.getString(R.string.app_name), Context.MODE_PRIVATE)
        val passsss = preferencepass.getString("password", "")
        val passcronoks = passsss.toString()
        passwordval = passcronoks
    }

    // update method

    // get update from navigation drawer
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(inputMessage: Message) { if (inputMessage.what == 0) {

            // Log.d("mioo", inputMessage.obj.toString())
            jsonString = inputMessage.obj.toString()
            // grab config version from stream
            val jsonObject = JSONObject(Crypt.parseToString(jsonString))
            val jsonarray = jsonObject.getJSONArray("Servers")
            val jsonversion = jsonObject.getString("Version")
            val jsonrelrasedfh = jsonObject.getString("ReleaseNotes")

            // get version from stored data
            val preference1=getSharedPreferences(resources.getString(R.string.app_name), MODE_PRIVATE)
            val jsoncopy1= preference1.getString("configrelease", "")

            if (jsoncopy1!! < jsonversion){
                ShowUpdateDialog(jsonversion, jsonarray, jsonrelrasedfh)
            }else{
                if (conditionstring.equals("true")){
                    ShowNoUpdateDialog()
                }else{

                }
            }
        }
        } }

    class Conn(mHand: Handler): Runnable {
        val myHandler = mHand
        override fun run() {
            var content = StringBuilder()
            try {
                // declare URL to text file, create a connection to it and put into stream.
                val myUrl = URL(Crypt.parseToString(BuildConfig.APPLICATIONFINGERPRINT))  // or URL to txt file
                val urlConnection = myUrl.openConnection() as HttpURLConnection
                val inputStream = urlConnection.inputStream

                // get text from stream, convert to string and send to main thread.
                val allText = inputStream.bufferedReader().use { it.readText() }
                content.append(allText)
                val str = content.toString()
                val msg = myHandler.obtainMessage()
                msg.what = 0
                msg.obj = str
                myHandler.sendMessage(msg)

            } catch (e: Exception) {
                Log.d("Error", e.toString())
            }
        }
    }

    private fun ShowNoUpdateDialog() {
        val noticetext = "<strong> CONFIG HAS BEEN UPDATED to LATEST VERSION  </strong> <br> <br> <br> <strong>TAKE NOTE !!!</strong> <br> <br> IF YOU WANT TO BUY THE PREMIUM KINDLY SEND US A MESSAGE <br> FB PROFILE: <br> https://www.facebook.com/cronoksphilkerii <br> <br> THANK YOU ALL  <br> <br> -THIS APP IS HAVE A ANTI MOD- <br> -MOD PA- <br> <br> <br> <br> <br> <strong> - FROM MENTAL VPN ADMIN- </strong>"
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.no_update_dialog)
        val body = dialog.findViewById(R.id.hadsTextView2) as TextView
        val sub = dialog.findViewById(R.id.hadsButton) as Button
        body.setText(Html.fromHtml(noticetext))

        sub.setOnClickListener {
            dialog.dismiss()

        }

        dialog.show()
    }

    private fun ShowUpdateDialog(jsonversion: String, jsonarray: JSONArray, jsonrelrasedfh: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.config_update_dialog)
        val body = dialog.findViewById(R.id.update_text2) as TextView
        val sub = dialog.findViewById(R.id.update_button) as Button
        body.setText("new Config Version Found ! \n Config Version: " + jsonversion + "\n \n  "+jsonrelrasedfh+"")

        sub.setOnClickListener {
            // save json servers
            val preference=getSharedPreferences(resources.getString(R.string.app_name), MODE_PRIVATE)
            val editor=preference.edit()
            editor.putString("json", jsonarray.toString())
            editor.commit()

            // save json server Version
            // server version & release notes
            val preference1=getSharedPreferences(resources.getString(R.string.app_name), MODE_PRIVATE)
            val editor1=preference1.edit()
            editor1.putString("configrelease", jsonversion)
            editor1.commit()
            val intent = intent
            // restart_app()
            // LoadServers(jsonString)
            startActivity(intent)
            dialog.dismiss()
            toast("Config Updated..")
            stop()
        }
        dialog.show()
    }

    private fun restart_app() {
        val intent = Intent(this, MainActivity::class.java)
        val i = 123456
        val pendingIntent =
            PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager[AlarmManager.RTC, System.currentTimeMillis() + 1000.toLong()] =
            pendingIntent
        finish()
    }

    private fun migrateLegacy() {
        GlobalScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.migrateLegacyConfig(this@MainActivity)
            if (result != null) {
                launch(Dispatchers.Main) {
                    if (result) {
                        toast(getString(R.string.migration_success))
                        mainViewModel.reloadServerList()
                    } else {
                        toast(getString(R.string.migration_fail))
                    }
                }
            }
        }
    }

    fun startV2Ray() {
        if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()) {
            return
        }
//        toast(R.string.toast_services_start)
        V2RayServiceManager.startV2Ray(this)
        V2RayServiceManager.titulongbahay(notificationconfigname)
    }

    fun netspeed(){
        if (mSubscription == null && V2RayServiceManager.v2rayPoint.isRunning) {
            var lastZeroSpeed = false
            val outboundTags = V2RayServiceManager.currentConfig?.getAllOutboundTags()
            outboundTags?.remove(AppConfig.TAG_DIRECT)

            mSubscription = Observable.interval(3, java.util.concurrent.TimeUnit.SECONDS)
                .subscribe {
                    val queryTime = System.currentTimeMillis()
                    val sinceLastQueryInSeconds = (queryTime - lastQueryTime) / 1000.0
                    var proxyTotal = 0L
                    val text = StringBuilder()
                    outboundTags?.forEach {
                        val up = V2RayServiceManager.v2rayPoint.queryStats(it, "uplink")
                        val down = V2RayServiceManager.v2rayPoint.queryStats(it, "downlink")
                        if (up + down > 0) {
                            appendSpeedString(text, it, up / sinceLastQueryInSeconds, down / sinceLastQueryInSeconds
                            )
                            proxyTotal += up + down
                        }
                    }
                    val directUplink = V2RayServiceManager.v2rayPoint.queryStats(AppConfig.TAG_DIRECT, "uplink")
                    val directDownlink = V2RayServiceManager.v2rayPoint.queryStats(AppConfig.TAG_DIRECT, "downlink")
                    val zeroSpeed = (proxyTotal == 0L && directUplink == 0L && directDownlink == 0L)
                    if (!zeroSpeed || !lastZeroSpeed) {
                        if (proxyTotal == 0L) {
                            appendSpeedString(text, outboundTags?.firstOrNull(), 0.0, 0.0
                            )
                        }
                        appendSpeedString(text, AppConfig.TAG_DIRECT, directUplink / sinceLastQueryInSeconds, directDownlink / sinceLastQueryInSeconds
                        )
                        val upload = findViewById<TextView>(R.id.uploadspeed) as TextView
                        var download = findViewById<TextView>(R.id.downloadspeed) as TextView

                        download.text = directDownlink.toString()
                        upload.text = directUplink.toString()
                    }
                    lastZeroSpeed = zeroSpeed
                    lastQueryTime = queryTime
                }
        }
    }

    private fun appendSpeedString(text: StringBuilder, name: String?, up: Double, down: Double) {
        var n = name ?: "no tag"
        n = n.substring(0, min(n.length, 6))
        text.append(n)
        for (i in n.length..6 step 2) {
            text.append("\t")
        }
        text.append("${up.toLong().toSpeedString()}↑  ${down.toLong().toSpeedString()}↓\n")
    }

    public override fun onResume() {
        super.onResume()
        mainViewModel.reloadServerList()
        // declare if v2ray is running dito sa resume time kase kapag binaback dumadagdag ung time for no reason
        // resumeTime()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.import_qrcode -> {
            importQRcode(true)
            true
        }
        R.id.import_clipboard -> {
            importClipboard()
            true
        }
        R.id.import_manually_vmess -> {
            startActivity(Intent().putExtra("createConfigType", EConfigType.VMESS.value).
            setClass(this, ServerActivity::class.java))
            true
        }
        R.id.import_manually_ss -> {
            startActivity(Intent().putExtra("createConfigType", EConfigType.SHADOWSOCKS.value).
            setClass(this, ServerActivity::class.java))
            true
        }
        R.id.import_manually_socks -> {
            startActivity(Intent().putExtra("createConfigType", EConfigType.SOCKS.value).
            setClass(this, ServerActivity::class.java))
            true
        }
        R.id.import_config_custom_clipboard -> {
            importConfigCustomClipboard()
            true
        }
        R.id.import_config_custom_local -> {
            importConfigCustomLocal()
            true
        }
        R.id.import_config_custom_url -> {
            importConfigCustomUrlClipboard()
            true
        }
        R.id.import_config_custom_url_scan -> {
            importQRcode(false)
            true
        }

//        R.id.sub_setting -> {
//            startActivity<SubSettingActivity>()
//            true
//        }

        R.id.sub_update -> {
            importConfigViaSub()
            true
        }

        R.id.export_all -> {
            if (AngConfigManager.shareNonCustomConfigsToClipboard(this, mainViewModel.serverList) == 0) {
                // toast(R.string.toast_success)
            } else {
                toast(R.string.toast_failure)
            }
            true
        }

        R.id.ping_all -> {
            mainViewModel.testAllTcping()
            true
        }

//        R.id.settings -> {
//            startActivity<SettingsActivity>("isRunning" to isRunning)
//            true
//        }
//        R.id.logcat -> {
//            startActivity<LogcatActivity>()
//            true
//        }
        else -> super.onOptionsItemSelected(item)
    }


    /**
     * import config from qrcode
     */
    fun importQRcode(forConfig: Boolean): Boolean {
//        try {
//            startActivityForResult(Intent("com.google.zxing.client.android.SCAN")
//                    .addCategory(Intent.CATEGORY_DEFAULT)
//                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), requestCode)
//        } catch (e: Exception) {
        RxPermissions(this)
                .request(Manifest.permission.CAMERA)
                .subscribe {
                    if (it)
                        if (forConfig)
                            scanQRCodeForConfig.launch(Intent(this, ScannerActivity::class.java))
                        else
                            scanQRCodeForUrlToCustomConfig.launch(Intent(this, ScannerActivity::class.java))
                    else
                        toast(R.string.toast_permission_denied)
                }
//        }
        return true
    }

    private val scanQRCodeForConfig = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            importBatchConfig(it.data?.getStringExtra("SCAN_RESULT"))
        }
    }

    private val scanQRCodeForUrlToCustomConfig = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            importConfigCustomUrl(it.data?.getStringExtra("SCAN_RESULT"))
        }
    }

    /**
     * import config from clipboard
     */
    fun importClipboard()
            : Boolean {
        try {
            val clipboard = Utils.getClipboard(this)
            importBatchConfig(clipboard)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importBatchConfig(server: String?, subid: String = "") {
        var count = AngConfigManager.importBatchConfig(server, subid)
        if (count <= 0) {
            count = AngConfigManager.importBatchConfig(Utils.decode(server!!), subid)
        }
        if (count > 0) {
            // toast(R.string.toast_success)
            mainViewModel.reloadServerList()
        } else {
            toast(R.string.toast_failure)
        }
    }

    fun importConfigCustomClipboard()
            : Boolean {
        try {
            val configText = Utils.getClipboard(this)
            if (TextUtils.isEmpty(configText)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            importCustomizeConfig(configText)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from local config file
     */
    fun importConfigCustomLocal(): Boolean {
        try {
            showFileChooser()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun importConfigCustomUrlClipboard()
            : Boolean {
        try {
            val url = Utils.getClipboard(this)
            if (TextUtils.isEmpty(url)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            return importConfigCustomUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * import config from url
     */
    fun importConfigCustomUrl(url: String?): Boolean {
        try {
            if (!Utils.isValidUrl(url)) {
                toast(R.string.toast_invalid_url)
                return false
            }
            GlobalScope.launch(Dispatchers.IO) {
                val configText = try {
                    Utils.getUrlContentWithCustomUserAgent(url)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
                launch(Dispatchers.Main) {
                    importCustomizeConfig(configText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * import config from sub
     */
    @SuppressLint("LongLogTag")
    fun importConfigViaSub()
            : Boolean {
        try {
            toast(R.string.title_sub_update)
            MmkvManager.decodeSubscriptions().forEach {
                if (TextUtils.isEmpty(it.first)
                        || TextUtils.isEmpty(it.second.remarks)
                        || TextUtils.isEmpty(it.second.url)
                ) {
                    return@forEach
                }
                val url = it.second.url
                if (!Utils.isValidUrl(url)) {
                    return@forEach
                }
                //  Log.d(ANG_PACKAGE, url)
                GlobalScope.launch(Dispatchers.IO) {
                    val configText = try {
                        Utils.getUrlContentWithCustomUserAgent(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            toast("\"" + it.second.remarks + "\" " + getString(R.string.toast_failure))
                        }
                        return@launch
                    }
                    launch(Dispatchers.Main) {
                        importBatchConfig(Utils.decode(configText), it.first)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * show file chooser
     */
    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            chooseFileForCustomConfig.launch(Intent.createChooser(intent, getString(R.string.title_file_chooser)))
        } catch (ex: android.content.ActivityNotFoundException) {
            toast(R.string.toast_require_file_manager)
        }
    }

    private val chooseFileForCustomConfig = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val uri = it.data?.data
        if (it.resultCode == RESULT_OK && uri != null) {
            readContentFromUri(uri)
        }
    }

    /**
     * read content from uri
     */
    private fun readContentFromUri(uri: Uri) {
        RxPermissions(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe {
                    if (it) {
                        try {
                            contentResolver.openInputStream(uri).use { input ->
                                importCustomizeConfig(input?.bufferedReader()?.readText())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else
                        toast(R.string.toast_permission_denied)
                }
    }

    /**
     * import customize config
     */
    fun importCustomizeConfig(server: String?) {
        try {
            if (server == null || TextUtils.isEmpty(server)) {
                toast(R.string.toast_none_data)
                return
            }
            mainViewModel.appendCustomConfigServer(server)
            // toast(R.string.toast_success)
            adapter.notifyItemInserted(mainViewModel.serverList.lastIndex)
        } catch (e: Exception) {
            ToastCompat.makeText(this, "${getString(R.string.toast_malformed_josn)} ${e.cause?.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            return
        }
    }

//    val mConnection = object : ServiceConnection {
//        override fun onServiceDisconnected(name: ComponentName?) {
//        }
//
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            sendMsg(AppConfig.MSG_REGISTER_CLIENT, "")
//        }
//    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
