package com.example.api_week1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button submitButton;
    private TextView resultText;
    private ImageView emojiView; // Thêm ImageView để hiển thị cảm xúc

    //private static final String API_KEY = "AIzaSyByqG57Bzks_dzvVD5CD3Vo9x1Zr1Id2uE";
    //private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
    private static final String API_URL = "https://a0ba-34-86-63-110.ngrok-free.app/predict";
    private static final String TAG = "API_DEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.inputText);
        submitButton = findViewById(R.id.submitButton);
        resultText = findViewById(R.id.resultText);
        emojiView = findViewById(R.id.emojiView); // Kết nối với ImageView trong XML

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText = editText.getText().toString();
                if (!inputText.isEmpty()) {
                    resultText.setText("Đang xử lý...");
                    emojiView.setVisibility(View.GONE); // Ẩn icon trước khi xử lý
                    sendRequest(inputText);
                } else {
                    resultText.setText("Vui lòng nhập nội dung!");
                }
            }
        });
    }

    private void sendRequest(String text) {
        Log.d(TAG, "Đang gửi request với nội dung: " + text);

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("text", text); // Chỉ gửi text thay vì cấu trúc phức tạp
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi tạo JSON request: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Lỗi khi gửi request: " + e.getMessage());
                runOnUiThread(() -> resultText.setText("Lỗi kết nối!"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "Response từ API: " + responseData);

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    String sentiment = jsonResponse.getString("sentiment"); // Đọc dữ liệu đúng key
                    runOnUiThread(() -> {
                        resultText.setText("Sentiment: " + sentiment);
                        updateEmoji(sentiment);
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Lỗi khi parse JSON: " + e.getMessage());
                    runOnUiThread(() -> resultText.setText("Lỗi xử lý dữ liệu!"));
                }
            }
        });
    }

    private String extractSentiment(String responseData) throws JSONException {
        Log.d(TAG, "Đang parse JSON response...");

        JSONObject jsonObject = new JSONObject(responseData);

        if (!jsonObject.has("candidates")) {
            Log.e(TAG, "Lỗi: JSON không chứa key 'candidates'");
            return "Không thể xác định";
        }

        JSONArray candidatesArray = jsonObject.getJSONArray("candidates");
        if (candidatesArray.length() == 0) {
            Log.e(TAG, "Lỗi: 'candidates' rỗng");
            return "Không thể xác định";
        }

        JSONObject firstCandidate = candidatesArray.getJSONObject(0);
        if (!firstCandidate.has("content")) {
            Log.e(TAG, "Lỗi: Không tìm thấy key 'content'");
            return "Không thể xác định";
        }

        JSONObject contentObject = firstCandidate.getJSONObject("content");
        if (!contentObject.has("parts")) {
            Log.e(TAG, "Lỗi: Không tìm thấy key 'parts'");
            return "Không thể xác định";
        }

        JSONArray partsArray = contentObject.getJSONArray("parts");
        if (partsArray.length() == 0) {
            Log.e(TAG, "Lỗi: 'parts' rỗng");
            return "Không thể xác định";
        }

        String sentimentText = partsArray.getJSONObject(0).optString("text", "Không thể xác định");

        Log.d(TAG, "Kết quả sentiment: " + sentimentText);
        return sentimentText.toLowerCase().contains("positive") ? "Tích cực" :
                sentimentText.toLowerCase().contains("negative") ? "Tiêu cực" :
                        "Trung lập";
    }

    private void updateEmoji(String sentiment) {
        if (sentiment.equals("Positive")) {
            emojiView.setImageResource(R.drawable.happy_face);
            emojiView.setVisibility(View.VISIBLE);
        } else if (sentiment.equals("Negative")) {
            emojiView.setImageResource(R.drawable.sad_face);
            emojiView.setVisibility(View.VISIBLE);
        } else {
            emojiView.setVisibility(View.GONE);
        }
    }
}