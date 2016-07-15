package com.kaszubski.kamil.emmhelper;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class XmlViewerActivity extends AppCompatActivity {
    private static final String TAG = "XMLViewerActivity";
    private TextView textView;
    private static HashMap<Integer, String> configMap;
    private static int mapSize;
    private ProgressDialog progressDialog;
    private String xml;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xml_viewer);
        textView = (TextView) findViewById(R.id.textView9);

        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.decoding_manifest_file));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        decodeXmlFile();
    }

    class DecodeXML extends AsyncTask<Void, Void, String>{
        @Override
        protected String doInBackground(Void... params) {
            try{
                String fileName = getIntent().getStringExtra(Constants.SOURCE_DIR);
                InputStream is = null;
                ZipFile zip = null;
                if (fileName.endsWith(".apk") || fileName.endsWith(".zip")) {
                    zip = new ZipFile(fileName);
                    ZipEntry mft = zip.getEntry("AndroidManifest.xml");
                    is = zip.getInputStream(mft);
                } else {
                    is = new FileInputStream(fileName);
                }
                byte[] buf = new byte[500000];
                int bytesRead = is.read(buf);
                is.close();
                if (zip != null) {
                    zip.close();
                }
                String xml;
                if(bytesRead >0){
                    xml = decompressXML(buf);
                    System.out.println(xml);
                } else {
                    xml = getString(R.string.reading_failed);
                }
                return xml.trim();
            }catch(Exception e){
                System.out.println(e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s != null)
                textView.setText(s);
            xml = s;
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.activity_xml_viewer, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_export_to_txt:

                String manifest = textView.getText().toString().trim();

                if(manifest.length() > 0){
                    Intent intent = new Intent(this, ExportActivity.class);
                    intent.putExtra(Constants.STRING_KEY, manifest);
                    intent.putExtra(Constants.FILE_FORMAT_KEY, Constants.TXT_FILE_EXTENSION);
                    startActivity(intent);
                } else {
                    Utils.showToast(this, getString(R.string.manifest_is_empty));
                }
                break;
            case R.id.action_share:
                if(xml != null)
                    Utils.shareViaList(this, xml);
                 else
                    Utils.showToast(this, getString(R.string.manifest_file_is_empty));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void decodeXmlFile(){
        new DecodeXML().execute();
    }

    public static int endDocTag = 0x00100101;
    public static int startTag = 0x00100102;
    public static int endTag = 0x00100103;

    public String decompressXML(byte[] xml) {
        StringBuilder finalXML = new StringBuilder();
        int level = 0;
        int numbStrings = LEW(xml, 4 * 4);
        int sitOff = 0x24; // Offset of start of StringIndexTable
        int stOff = sitOff + numbStrings * 4; // StringTable follows
        int xmlTagOff = LEW(xml, 3 * 4); // Start from the offset in the 3rd

        for (int ii = xmlTagOff; ii < xml.length - 4; ii += 4) {
            if (LEW(xml, ii) == startTag) {
                xmlTagOff = ii;
                break;
                }
            } // end of hack, scanning for start of first start tag
        boolean wasEndTagPreviously = false;
        boolean firstLine = true;
        int off = xmlTagOff;
        int indent = 0;
        int startTagLineNo = -2;
        String tab = "      ";
        while (off < xml.length) {
            int tag0 = LEW(xml, off);
            int lineNo = LEW(xml, off + 2 * 4);
            int nameNsSi = LEW(xml, off + 4 * 4);
            int nameSi = LEW(xml, off + 5 * 4);
            if (tag0 == startTag) { // XML START TAG
                if(!firstLine){
                    if(!wasEndTagPreviously)
                        finalXML.append(">").append("\n").append("\n");
                    else
                        finalXML.append("\n");
                }
                else {
                    firstLine = false;
                }

                int tag6 = LEW(xml, off + 6 * 4); // Expected to be 14001400
                int numbAttrs = LEW(xml, off + 7 * 4); // Number of Attributes
                off += 9 * 4; // Skip over 6+3 words of startTag data
                String name = compXmlString(xml, sitOff, stOff, nameSi);
                startTagLineNo = lineNo;
                // Look for the Attributes
                StringBuilder sb = new StringBuilder();
                boolean firstAttr = true;
                for (int ii = 0; ii < numbAttrs; ii++) {
                    int attrNameNsSi = LEW(xml, off); // AttrName Namespace Str
                    int attrNameSi = LEW(xml, off + 4); // AttrName String
                    int attrValueSi = LEW(xml, off + 2 * 4); // AttrValue Str
                    int attrFlags = LEW(xml, off + 3 * 4);
                    int attrResId = LEW(xml, off + 4 * 4); // AttrValue
                    off += 5 * 4; // Skip over the 5 words of an attribute
                    String attrName = compXmlString(xml, sitOff, stOff, attrNameSi);

                    Log.e(TAG, "name "+attrName + " flag " + attrFlags + " val " + attrResId);

                    String attrValue = attrValueSi != -1 ? compXmlString(xml,
                    sitOff, stOff, attrValueSi) : decodeAttrValue(attrName, attrFlags, attrResId);

                    if(!firstAttr){
                        sb.append("\n");
                        for(int i = 0; i<level+1; i++)
                            sb.append(tab);
                    } else {
                        firstAttr = false;
                        sb.append(" ");
                    }


                    sb.append(attrName).append("=\"").append(attrValue).append("\"");
                }
                for(int i = 0; i<level; i++)
                    finalXML.append(tab);
                level++;

                //finalXML.append("<").append(name).append(sb).append(">");
                finalXML.append("<").append(name).append(sb);
                prtIndent(indent, "<" + name + sb + ">");
                indent++;
                wasEndTagPreviously = false;
            } else if (tag0 == endTag) { // XML END TAG
                indent--;
                level--;
                off += 6 * 4; // Skip over 6 words of endTag data
                String name = compXmlString(xml, sitOff, stOff, nameSi);
                if(wasEndTagPreviously) {
                    finalXML.append("\n");
                    for(int i = 0; i<level; i++)
                        finalXML.append(tab);
                    finalXML.append("</").append(name).append(">").append("\n");
                } else
                    finalXML.append("/>").append("\n");
//                finalXML.append("</").append(name).append(">");
                prtIndent(indent, "</" + name + "> (line " + startTagLineNo + "-" + lineNo + ")");
                wasEndTagPreviously = true;
            } else if (tag0 == endDocTag) { // END OF XML DOC TAG
                break;
            } else {
                prt("  Unrecognized tag code '" + Integer.toHexString(tag0) + "' at offset " + off);
                break;
                }
            } // end of while loop scanning tags and attributes of XML tree
                return finalXML.toString();
        } // end of decompressXML

    private static String decodeAttrValue(String attrName, int type, int value){
        switch(type){
            case 268435464: //number
                switch (attrName){
                    case "screenOrientation":
                        return getScreenOrientationName(value);
                    case "launchMode":
                        return getLaunchModeName(value);
                    default:
                        return String.valueOf(value);
//                        return  "resourceID 0x" + Integer.toHexString(value);
                }
            case 285212680:
                switch (attrName){
                    case "windowSoftInputMode":
                        return getWindowSoftInputModeName(value);
                    case "protectionLevel":
                        return getProtectionLevelName(value);
                    case "configChanges":
                        return getConfigChangesName(value);
                    default:
//                        return String.valueOf(value);
                        return  "resourceID 0x" + Integer.toHexString(value);
                }

            case 301989896: //boolean
                return value == -1 ? "false" : "true";
            default:
                return  "resourceID 0x" + Integer.toHexString(value);
        }
    }

    private static String getScreenOrientationName(int value){
        switch (value) {
            case -1:
                return "unspecified";
            case 0:
                return "landscape";
            case 1:
                return "portrait";
            case 2:
                return "user";
            case 3:
                return "behind";
            case 4:
                return "sensor";
            case 5:
                return "nosensor";
            case 6:
                return "sensorLandscape";
            case 7:
                return "sensorPortrait";
            case 8:
                return "reverseLandscape";
            case 9:
                return "reversePortrait";
            case 10:
                return "fullSensor";
            case 11:
                return "userLandscape";
            case 12:
                return "userPortrait";
            case 13:
                return "fullUser";
            case 14:
                return "locked";
            default:
                return "unspecified";
        }
    }

    private static String getLaunchModeName(int value){
        switch (value) {
            case 0:
                return "standard";
            case 1:
                return "singleTop";
            case 2:
                return "singleTask";
            case 3:
                return "singleInstance";
            default:
                return "standard";
        }
    }

    private static String getWindowSoftInputModeName(int value){
        switch (value) {
            case 0:
                return "stateUnspecified";
            case 1:
                return "stateUnchanged";
            case 2:
                return "stateHidden";
            case 3:
                return "stateAlwaysHidden";
            case 4:
                return "stateVisible";
            case 5:
                return "stateAlwaysVisible";
            case 16:
                return "adjustResize";
            case 32:
                return "adjustPan";
            default:
                return "adjustUnspecified";
        }
    }

    private static String getProtectionLevelName(int value){
        switch (value) {
            case 0:
                return "normal";
            case 1:
                return "dangerous";
            case 2:
                return "signature";
            case 3:
                return "signatureOrSystem";
            case 16:
                return "privileged";
            default:
                return "normal";
        }
    }

    private static String getConfigChangesName(int value){
        if(configMap == null)
            initConfigMap();

        int valueToCheck = value;
        Integer[] keyValues = new Integer[mapSize];
        configMap.keySet().toArray(keyValues);
        ArrayList<Integer> keyValuesList = new ArrayList<>(mapSize);
        keyValuesList.addAll(Arrays.asList(keyValues));
        Collections.sort(keyValuesList);
        StringBuilder builder = new StringBuilder();

        while (valueToCheck != 0) {
            for (int i = 0; i < mapSize; i++) {

                if(valueToCheck < keyValuesList.get(i)){

                    valueToCheck -= keyValuesList.get(i-1);
                    builder.append(configMap.get(keyValuesList.get(i-1))).append(" | ");
                    break;
                }
                if(i == mapSize-1){
                    valueToCheck -= keyValuesList.get(mapSize-1);
                    builder.append(configMap.get(keyValuesList.get(mapSize-1))).append(" | ");
                    break;
                }
            }
        }
        return builder.toString().substring(0, builder.length()-3);
    }

    private static void initConfigMap(){
        configMap = new HashMap<>(15);
        configMap.put(1, "mcc");
        configMap.put(2, "mnc");
        configMap.put(4, "locale");
        configMap.put(8, "touchscreen");
        configMap.put(16, "keyboard");
        configMap.put(32, "keyboardHidden");
        configMap.put(64, "navigation");
        configMap.put(128, "orientation");
        configMap.put(256, "screenLayout");
        configMap.put(512, "uiMode");
        configMap.put(1024, "screenSize");
        configMap.put(2048, "smallestScreenSize");
        configMap.put(4096, "density");
        configMap.put(8192, "layoutDirection");
        configMap.put(1073741824, "fontScale");

        mapSize = configMap.size();
    }

    public static String compXmlString(byte[] xml, int sitOff, int stOff, int strInd) {
        if (strInd < 0)
            return null;
        int strOff = stOff + LEW(xml, sitOff + strInd * 4);
        return compXmlStringAt(xml, strOff);
    }

    public static String spaces = "                                             ";

    public static void prtIndent(int indent, String str) {
        prt(spaces.substring(0, Math.min(indent * 2, spaces.length())) + str);
    }

    static void prt(String str) {
        System.err.print(str);
    }

    public static String compXmlStringAt(byte[] arr, int strOff) {
        int strLen = arr[strOff + 1] << 8 & 0xff00 | arr[strOff] & 0xff;
        byte[] chars = new byte[strLen];
        for (int ii = 0; ii < strLen; ii++) {
            chars[ii] = arr[strOff + 2 + ii * 2];
            }
        return new String(chars); // Hack, just use 8 byte chars
    } // end of compXmlStringAt

    public static int LEW(byte[] arr, int off) {
        return arr[off + 3] << 24 & 0xff000000 | arr[off + 2] << 16 & 0xff0000 | arr[off + 1] << 8 & 0xff00 | arr[off] & 0xFF;
    } // end of LEW
}
