package burp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.awt.Component;
import java.util.Arrays;

public class BurpExtender implements IBurpExtender, IMessageEditorTabFactory {

    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;

    //
    // implement IBurpExtender
    //
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        // keep a reference to our callbacks object
        this.callbacks = callbacks;

        // obtain an extension helpers object
        helpers = callbacks.getHelpers();

        // set our extension name
        callbacks.setExtensionName("JSON Decoder");

        // register ourselves as a message editor tab factory
        callbacks.registerMessageEditorTabFactory(this);
    }

    //
    // implement IMessageEditorTabFactory
    //
    @Override
    public IMessageEditorTab createNewInstance(IMessageEditorController controller, boolean editable) {
        // create a new instance of our custom editor tab
        return new JSONDecoderTab(controller, editable);
    }

    //
    // class implementing IMessageEditorTab
    //
    class JSONDecoderTab implements IMessageEditorTab {

        private boolean editable;
        private ITextEditor txtInput;
        private byte[] currentMessage;

        public JSONDecoderTab(IMessageEditorController controller, boolean editable) {
            this.editable = editable;

            // create an instance of Burp's text editor, to display our deserialized data
            txtInput = callbacks.createTextEditor();
            txtInput.setEditable(editable);
        }

        //
        // implement IMessageEditorTab
        //
        @Override
        public String getTabCaption() {
            return "JSON Decoder";
        }

        @Override
        public Component getUiComponent() {
            return txtInput.getComponent();
        }

        @Override
        public boolean isEnabled(byte[] content, boolean isRequest) {
            IRequestInfo requestInfo;
            IResponseInfo responseInfo;
            if (isRequest) {
                requestInfo = helpers.analyzeRequest(content);
                return requestInfo.getContentType() == IRequestInfo.CONTENT_TYPE_JSON;
            } else {
                responseInfo = helpers.analyzeResponse(content);
                return responseInfo.getInferredMimeType().equals("JSON");
            }
        }

        @Override
        public void setMessage(byte[] content, boolean isRequest) {
            String json = "";
            if (content == null) {
                // clear our display
                txtInput.setText("none".getBytes());
                txtInput.setEditable(false);
            } else {
                //Take the input, determine request/response, parse as json, then print prettily.
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                int bodyOffset = 0;
                if (isRequest) {
                    IRequestInfo requestInfo = helpers.analyzeRequest(content);
                    bodyOffset = requestInfo.getBodyOffset();

                } else {
                    IResponseInfo responseInfo = helpers.analyzeResponse(content);
                    bodyOffset = responseInfo.getBodyOffset();

                }
                byte[] requestResponseBody = Arrays.copyOfRange(content, bodyOffset, content.length);
                try {
                    JsonParser jp = new JsonParser();
                    JsonElement je = jp.parse(new String(requestResponseBody));
                    json = gson.toJson(je);
                    txtInput.setText(json.getBytes());
                    txtInput.setEditable(editable);
                } catch (Exception e) {
                    txtInput.setText(e.toString().getBytes());
                }

            }

            // remember the displayed content
            currentMessage = json.getBytes();
        }

        @Override
        public byte[] getMessage() {
            return null;
        }

        @Override
        public boolean isModified() {
            return txtInput.isTextModified();
        }

        @Override
        public byte[] getSelectedData() {
            return txtInput.getSelectedText();
        }
    }
}
