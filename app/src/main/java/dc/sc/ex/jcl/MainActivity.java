package dc.sc.ex.jcl;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
/**
 * This app is designed to demonstrate the execution of a custom sound command from
 * a dolphin (or x-user) which is recognized by DC Dolphin Comm app (DC) and sent to this app.
 * It receives, from DC, messages (Intent instances) containing soundCommand String, date-time,
 * etc., and sends messages containing results of the execution to DC.
 * DC then displays the results to h-user in it's gui.
 * A third-party instance of such and executor app may not send results to DC, which would be fine.
 *
 * <p>JCL = John Cunningham Lilly, who is one of the few humans who have seriously tried to
 * communicate with dolphins,
 * and who stopped hurting dolphins when he realized that they were sentient,
 * at a time when deadly experimentation with animals were common and ethically acceptable.</p>
 *
 * <p>Release 1 = 2022-3-5</p>
 *
 * @since 2022-3-5
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final boolean LOG = false; //todo false in prod
    /**
     * values:
     * sound-command-from-x-via-dc
     * sound-command-results-to-dc
     * sound-command-ack-to-dc
     */
    static final String SOUND_COMMAND_INTENT_EXTRA_TYPE = "sm.app.dc.intent.extra.TYPE";
    static final String SOUND_COMMAND_INTENT_TYPE_FROM_X_VIA_DC = "sound-command-from-x-via-dc";
    static final String SOUND_COMMAND_INTENT_TYPE_RESULTS_TO_DC = "sound-command-results-to-dc";
    /**
     * for future use: to notify DC that the executor app has received a sound command.
     */
    static final String SOUND_COMMAND_INTENT_TYPE_ACK_TO_DC = "sound-command-ack-to-dc";
    static final String SOUND_COMMAND_INTENT_EXTRA_CMD = "sm.app.dc.intent.extra.SOUND_COMMAND";
    /**
     * this SC_ID attribute-value-pair is designed to the included in the results sent to DC.
     */
    static final String SOUND_COMMAND_INTENT_EXTRA_SC_ID ="sm.app.dc.intent.extra.SOUND_COMMAND_ID";
    static final String SOUND_COMMAND_INTENT_EXTRA_DATE_STRING =
            "sm.app.dc.intent.extra.SOUND_COMMAND_DATE_STRING";
    static final String SOUND_COMMAND_INTENT_EXTRA_TIME_MILLIS =
            "sm.app.dc.intent.extra.SOUND_COMMAND_TIME_MILLIS";
    /**
     * @deprecated not used, kept for possible future use
     */
    static final String SOUND_COMMAND_INTENT_EXTRA_INSTALLATION_ID =
            "sm.app.dc.intent.extra.SOUND_COMMAND_INSTALLATION_ID";
    static final String SOUND_COMMAND_INTENT_EXTRA_APP_ID =
            "sm.app.dc.intent.extra.SOUND_COMMAND_APP_ID";
    static final String SOUND_COMMAND_INTENT_EXTRA_RESULTS =
            "sm.app.dc.intent.extra.SOUND_COMMAND_RESULTS";
    static final String SOUND_COMMAND_INTENT_EXTRA_RESULTS_SUCCESS =
            "sm.app.dc.intent.extra.SOUND_COMMAND_RESULTS_SUCCESS";

    String soundCommandFromX = "sc-test";
    String soundCommandId = "";
    TextView textViewCommandData = null;
    TextView textViewExecResults = null;
    EditText editTextManualResults = null;
    Button buttonSendSuccess = null;
    Button buttonSendFailure = null;
    TextView log = null;
    Button buttonExecute = null;
    ToggleButton toggleButtonExecuteImmediately = null;
    ToggleButton toggleButtonSendImmediately = null;
    public static final boolean EXECUTE_IMMEDIATELY_DEFAULT = false;
    public static final boolean SEND_IMMEDIATELY_DEFAULT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            TextView tv1 = findViewById(R.id.top_view_jcl_main);
            if(tv1!=null) {
                int v = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                tv1.setText("v"+v+" "+tv1.getText());
            }
            textViewCommandData = findViewById(R.id.textViewCommandData);
            textViewExecResults = findViewById(R.id.textViewResults);
            log = findViewById(R.id.jcl_log_tv);
            editTextManualResults = findViewById(R.id.editTextManualResults);
            if(editTextManualResults!=null){
                editTextManualResults.setSelected(false);
                // to prevent keyboard to show at startup:
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                editTextManualResults.setFocusableInTouchMode(true);
                editTextManualResults.setEnabled(true);
            }
            buttonSendSuccess = findViewById(R.id.buttonSendSuccess);
            buttonSendSuccess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (LOG) {
                            Log.d(TAG, "buttonSendSuccess: onClick, entering...");
                        }
                        sendResultsWithExplicitIntent(true);
                    } catch (Throwable e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        writeInLog("buttonSendSuccess listener onClick: " + e + "\n" + sw);
                        Log.e(TAG, "buttonSendSuccess listener onClick: " + e + "\n" + sw);
                        appendToResults("buttonSendSuccess listener onClick: anomaly = " + e
                                + "\n" + sw);
                    }
                }
            });
            buttonSendFailure = findViewById(R.id.buttonSendFailure);
            buttonSendFailure.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        sendResultsWithExplicitIntent(false);
                    } catch (Throwable e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        Log.e(TAG, "buttonSendFailure listener onClick: " + e + "\n" + sw);
                        appendToResults("buttonSendFailure listener onClick: anomaly = " + e
                                + "\n" + sw);
                    }
                }
            });
            buttonExecute = findViewById(R.id.buttonExecute);
            buttonExecute.setOnClickListener(v -> executeSoundCommand(intentFromDC));
            toggleButtonExecuteImmediately = findViewById(R.id.toggleButtonExecuteImmediately);
            toggleButtonSendImmediately = findViewById(R.id.toggleButtonSendImmediately);

            // notify absence or presence of dc on device
            if(isDCOnDevice()){
                writeInLog("DC app is on this device.");
            }else{
                writeInLog("DC app is _not_ on this device" +
                        "; please download it from Google Play(TM)" +
                        ": *DC Dolphin Communicator*");
                Toast.makeText(this, "DC app is _not_ on this device; download it.",
                        Toast.LENGTH_LONG).show();
            }
        }catch(Throwable e){
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            writeInLog("buttonSendSuccess listener onClick: " + e + "\n" + sw);
            if(LOG) {
                Log.e(TAG, "buttonSendSuccess listener onClick: " + e + "\n" + sw);
            }
        }
    }

    Intent intentFromDC = null;

    private void receiveSoundCommandWithExplicitIntent(){
        intentFromDC = getIntent();
        String action = intentFromDC.getAction(); //now this is Intent.ACTION_MAIN
        if(Intent.ACTION_MAIN.equals(action)){
            //normal
        }else{
            //abnormal
            appendToResults("Intent action is unexpected = {"+action+"}");
        }
        String msgType = intentFromDC.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_TYPE);
        writeInLog("receiveSoundCommandWithExplicitIntent" +
                ": entering with msgType = {"+msgType+"}");
        if(SOUND_COMMAND_INTENT_TYPE_FROM_X_VIA_DC.equals(msgType)) {
            if(LOG){
                Log.d(TAG,"receiveSoundCommandWithExplicitIntent" +
                        ": entering with valid Intent action");
            }
            soundCommandId = intentFromDC.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_SC_ID);
            soundCommandFromX = intentFromDC.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_CMD);
            writeInLog("receiveSoundCommandWithExplicitIntent" +
                    ": soundCommandFromX = {"+soundCommandFromX+"}"+
                    " soundCommandId = {"+soundCommandId+"}");
            if(soundCommandFromX==null||soundCommandFromX.length()==0){
                // anomaly
                appendToResults("The received msg does not contain a sound command.");
            }else {
                // ok
                writeReceivedIntent(intentFromDC, this);
                if(toggleButtonExecuteImmediately.isChecked()) {
                    //Immediate: the big work on bg thread
                    executeSoundCommand(intentFromDC);
                }
            }
        }else{
            if(LOG) Log.w(TAG,"receiveSoundCommandWithExplicitIntent" +
                    ": start without an Intent from DC; " +
                    "\n action: {"+action+"}"+ "\n msgType: {"+msgType+"}");
            appendToResults("Msg type not from DC: {"+msgType+"}");
        }
    }

    private void executeSoundCommand(final Intent intentFromDC){
        writeInLog("executeSoundCommand: entering with intentFromDC = {"+intentFromDC+"}");
        if(toggleButtonSendImmediately==null){
            writeInLog("defect detected in executeSoundCommand: " +
                    "toggleButtonSendImmediately is null; exiting");
            return;
        }
        if(textViewExecResults!=null) {
            final boolean SEND_IMMEDIATE = toggleButtonSendImmediately.isChecked();
            textViewExecResults.setText("");
            new Thread() {
                public void run() {
                    try {
                        executeSoundCommandOnBgThread(intentFromDC,SEND_IMMEDIATE);
                    }catch(Throwable e){
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        String s = "executeSoundCommand: " +
                                "in thread calling executeSoundCommandOnBgThread: " + e +
                                "\n" + sw;
                        Log.e(TAG, s);
                        writeInLog(s);
                        appendToResults(s);
                    }
                }
            }.start();
        }else{
            writeInLog("defect detected in executeSoundCommand: " +
                    "textViewExecResults is null; exiting");
        }
    }

    /**
     * Designed to be called on a background thread.
     *
     * <p>In future, results may include a list of signals to be emitted by dc:
     * !emit signal1 signal2 signal3...
     * </p>
     */
    private void executeSoundCommandOnBgThread(final Intent intent, final boolean sendImmediately){
        try {
            if(intent!=null) {
                soundCommandFromX = intent.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_CMD);
                soundCommandId = intent.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_SC_ID);
                writeInLog("executeSoundCommandOnBgThread: entering with Intent containing " +
                        "soundCommandFromX = {"+ soundCommandFromX+"}"+
                        " soundCommandId = {"+soundCommandId+"}");
            }else{
                writeInLog("executeSoundCommandOnBgThread: entering with no Intent and " +
                        "soundCommandFromX = {"+ soundCommandFromX+"}");
            }
            appendToResults("Starting the execution of sound command {" +
                    soundCommandFromX + "}.");

            //todo write your code here to process the sound command
            // and edit the following statements for your own needs

            appendToResults("Nothing else is done here." +
                    "The implementer would normally add the custom code here." +
                    " This could be, for example, to execute a program" +
                    ", or to send an http request to a web address" +
                    ", or to send an email to staff.");

            boolean success = true;

            // the results are sent by h-user using the buttons (success or failure)
            // after going to the receiver app manually (this app);
            // this is seen by the h-user after opening this app manually
            appendToResults("The execution of sound command {" +
                    soundCommandFromX + "} has completed.");

            if(sendImmediately) {
                writeInLog("executeSoundCommandOnBgThread: sendImmediately is checked, " +
                        "so sending now");
                //============================================
                sendResultsWithExplicitIntent(success);
                //============================================
            }else{
                writeInLog("executeSoundCommandOnBgThread: sendImmediately is not checked, " +
                        "so not sending now and waiting for the manual action");
            }
        }finally {
            writeInLog("executeSoundCommandOnBgThread: exiting");
        }
    }

    public static final String DC_ACTIVITY = "sm.app.dc/.CustomSoundCommandResultsReceiver";

    boolean isDCOnDevice(){
        Intent explicit = new Intent();
        ComponentName cn = ComponentName.unflattenFromString(DC_ACTIVITY);
        explicit.setComponent(cn);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(explicit, 0);
        return ! list.isEmpty();
    }

    public static final String AUTOMATED_RESULTS_LABEL = "Automated Results";
    public static final String MANUAL_RESULTS_LABEL = "Manual Results";
    /**
     * Uses an explicit Intent
     */
    private void sendResultsWithExplicitIntent(final boolean success){
        try {
            final String s = success ? "success" : "failure";
            Intent explicit = new Intent();
            ComponentName cn = ComponentName.unflattenFromString(DC_ACTIVITY);
            explicit.setComponent(cn);
            String execResults = textViewExecResults.getText().toString();
            if(TextUtils.isEmpty(execResults))execResults = "(empty)";
            String manualResults = editTextManualResults.getText().toString();
            if(TextUtils.isEmpty(manualResults))manualResults = "(empty)";
            String results = "[\n"+AUTOMATED_RESULTS_LABEL+": " + execResults +
                    "\n][\n"+MANUAL_RESULTS_LABEL+": " + manualResults+"\n]";
            addExtras(explicit, results, success);
            PackageManager pm = getPackageManager();
            //ComponentName resolved = explicit.resolveActivity(pm);//todo needed???
            //todo try queryIntentActivities and queryIntentActivityOptions
            List<ResolveInfo> list = pm.queryIntentActivities(explicit, 0);
//            writeInLog("sendResultsWithExplicitIntent: List<ResolveInfo> list {"+list+"}"+
//                " for "+DC_ACTIVITY);
            if (list.isEmpty()) {
                writeInLog("The " + s + " results were NOT sent to DC " +
                        "because DC app not found on this device" +
                        "; you need to download it from Google Play(TM): "+
                        "*DC Dolphin Communicator*");
                return;
            } else {
                try {
                    startActivity(explicit);
                } catch (ActivityNotFoundException e) {
                    writeInLog("Results NOT sent to DC " +
                            "because *startActivity(explicit)* raised: " + e);
                    return;
                }
                writeInLog("The " + s + " results were sent to DC.");
            }
        }catch(Throwable e2){
            writeInLog("Anomaly in sendResultsWithExplicitIntent: " + e2);
        }
    }

    void addExtras(final Intent intent, final String results, final boolean success){
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_TYPE,SOUND_COMMAND_INTENT_TYPE_RESULTS_TO_DC);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_RESULTS,results);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_RESULTS_SUCCESS,""+success);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_CMD,soundCommandFromX);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_SC_ID,soundCommandId);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_TIME_MILLIS,System.currentTimeMillis());
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_DATE_STRING,""+Calendar.getInstance().getTime());
//        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_INSTALLATION_ID,"to do");
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_APP_ID,getClass().getName());
    }

    /**
     * for future use;
     * input: soundCommandFromX
     */
    Intent buildIntentForReceptionNotifToDC(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_TYPE,SOUND_COMMAND_INTENT_TYPE_ACK_TO_DC);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_CMD,soundCommandFromX);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_SC_ID,soundCommandId);
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_TIME_MILLIS,System.currentTimeMillis());
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_DATE_STRING,""+Calendar.getInstance().getTime());
//        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_INSTALLATION_ID,"to do");
        intent.putExtra(SOUND_COMMAND_INTENT_EXTRA_APP_ID,getClass().getName());
        return intent;
    }

    /**
     * Designed to be called when receiving a potentially valid Intent.
     *
     * @param intent the received Intent
     * @param context from Android when receiving an Intent.
     */
    void writeReceivedIntent(final Intent intent, final Context context){
        soundCommandFromX = intent.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_CMD);
        writeInLog("Received sound command {" + soundCommandFromX+"}");
        String dateString = intent.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_DATE_STRING);
        long timeMillisLong = intent.getLongExtra(SOUND_COMMAND_INTENT_EXTRA_TIME_MILLIS,0L);
//        String installationId = intent.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_INSTALLATION_ID);
        String appId = intent.getStringExtra(SOUND_COMMAND_INTENT_EXTRA_APP_ID);
        //show reception of sound command from DC in gui
        final String s = "Sound command received = {" + soundCommandFromX + "}" +
                "\n\nsound command id = {"+soundCommandId+"}"+
                "\n\ndate = {" + dateString + "}" +
                "\n\ntime ms = " + timeMillisLong +
//                "\n\ninstallation id = {" + installationId + "}" +
                "\n\napp id = {" + appId + "}";
        if(textViewCommandData !=null) {
            textViewCommandData.post(() -> textViewCommandData.setText(s));
        }else{
            writeInLog("writeReceivedIntent(2-args): textViewCommandData is null");
        }
    }

    /**
     * Designed to be called by the method that executes the sound command, for example
     * for a result or an anomaly in the received Intent.
     *
     * @param text
     */
    void appendToResults(String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (textViewExecResults != null) {
                    String prev = textViewExecResults.getText().toString();
                    if (TextUtils.isEmpty(prev)) {
                        textViewExecResults.setText(text);
                    } else {
                        textViewExecResults.setText(prev + "\n~~~~~\n" + text);
                    }
                } else {
                    MainActivity.this.writeInLog("appendToResults(1-arg): " +
                            "anomaly textViewExecResults is null");
                }
            }
        });
    }

    void clearResults(){
        runOnUiThread(() -> {
            if(textViewExecResults==null){
                writeInLog("clearResults(): anomaly textViewExecResults is null");
                return;
            }
            textViewExecResults.setText("");
        });
    }

    /**
     * works also when called on a bg thread.
     *
     * @param text
     */
    private void writeInLog(final String text){
        if(log==null)return;
        log.post(() -> {
            CharSequence l = log.getText();
            if(l==null) l = "";
            String s = "";
            if(TextUtils.isEmpty(l)){
                s = getDateTimePrefixForLog()+": "+text;
            }else{
                s = l +"\n"+getDateTimePrefixForLog()+": "+text;
            }
            log.setText(s);
        });
    }
    private boolean firstLogLineInSession = true;
    private String getDateTimePrefixForLog() {
        if(firstLogLineInSession) {
            firstLogLineInSession = false;
            return getDateTimeShort(System.currentTimeMillis());
        }else{
            return getTimeShort(System.currentTimeMillis());
        }
    }
    static String getDateTimeShort(final long millis){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(millis);
    }
    static String getTimeShort(final long millis){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(millis);
    }

    //============== preferences ==============


    /**
     * Designed to be called after the GUI is created.
     */
    void restorePreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean exec = preferences.getBoolean(TOGGLE_BUTTON_EXECUTE_IMMEDIATELY_KEY,
                EXECUTE_IMMEDIATELY_DEFAULT);
        if(toggleButtonExecuteImmediately!=null) {
            toggleButtonExecuteImmediately.setChecked(exec);
        }
        boolean send = preferences.getBoolean(TOGGLE_BUTTON_SEND_IMMEDIATELY_KEY,
                SEND_IMMEDIATELY_DEFAULT);
        if(toggleButtonSendImmediately!=null){
            toggleButtonSendImmediately.setChecked(send);
        }
    }

    public static final String TOGGLE_BUTTON_EXECUTE_IMMEDIATELY_KEY =
            "toggleButtonExecuteImmediatelyKey";
    public static final String TOGGLE_BUTTON_SEND_IMMEDIATELY_KEY =
            "toggleButtonSendImmediatelyKey";

    void savePreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        boolean exec = toggleButtonExecuteImmediately==null?EXECUTE_IMMEDIATELY_DEFAULT:
                toggleButtonExecuteImmediately.isChecked();
        editor.putBoolean(TOGGLE_BUTTON_EXECUTE_IMMEDIATELY_KEY,exec);

        boolean send = toggleButtonSendImmediately==null?SEND_IMMEDIATELY_DEFAULT:
                toggleButtonSendImmediately.isChecked();
        editor.putBoolean(TOGGLE_BUTTON_SEND_IMMEDIATELY_KEY,send);

        editor.commit();
    }
    protected void onResume(){
        super.onResume();
        writeInLog("onResume: starting with: " +
                "\n EXECUTE_IMMEDIATELY_DEFAULT = "+ EXECUTE_IMMEDIATELY_DEFAULT+
                "\n SEND_IMMEDIATELY_DEFAULT = "+SEND_IMMEDIATELY_DEFAULT
        );
        restorePreferences();
        writeInLog("onResume: restored preferences:"+
                "\n toggleButtonExecuteImmediately.isChecked() = "+
                toggleButtonExecuteImmediately.isChecked()+
                "\n toggleButtonSendImmediately.isChecked() = " +
                toggleButtonSendImmediately.isChecked()
        );
        clearResults();
        receiveSoundCommandWithExplicitIntent();
    }
    protected void onPause(){
        savePreferences();
        super.onPause();
    }
    protected void onStop(){
        savePreferences();
        super.onStop();
    }
    protected void onDestroy(){
        savePreferences();
        super.onDestroy();
    }
}