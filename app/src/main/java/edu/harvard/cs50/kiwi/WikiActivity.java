package edu.harvard.cs50.kiwi;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.lang.String;
import java.util.Objects;

public class WikiActivity extends AppCompatActivity {

    private List<String> wordCombs = new ArrayList<>();
    private List<String> wordsFound = new ArrayList<>();
    private Map<String, String> wordLinksMap = new HashMap<>();
    private int requestCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki);

        // Display logo in toolbar
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.kiwi_logo);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        // Get user input text for analysis
        String userText = getIntent().getStringExtra("userText");
        List<String> stopWords = Arrays.asList(getString(R.string.stopWords).split("\\s*,\\s*"));

        // Extract all titles from text
        assert userText != null;
        List<String> titles = getAllTitles(userText, stopWords);

        // Search Wikipedia for article titles matching one or more keywords
        try {
            searchWiki(titles);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllTitles(String userText, List<String> stopWords) {
        String userTextSanitized = userText.replaceAll("[^a-zA-Z0-9\\s+]", "");
        List<String> words = new ArrayList<>(Arrays.asList(userTextSanitized.split("\\s+")));

        // Eliminate all stop words from list and get all consecutive word combinations
        // of size k (to be able to search for names, etc.)
        List<String> titles = getAllCombs(words, stopWords, 3);

        // Get all word cases (all lowercase, all uppercase and name cases)
        titles.addAll(getAllCases(titles, stopWords));

        return titles;
    }

    public List<String> getAllCombs(List<String> words, List<String> stopWords, int k) {
        // Loop over list of words
        for (int i = 0; i < words.size(); i++) {
            String word1 = words.get(i).trim();
            if (!stopWords.contains(word1.toLowerCase())) {
                if (!wordCombs.contains(word1)) {
                    wordCombs.add(word1);
                }
                // If not in stop word or the last word
                if (i != words.size()) {
                    int limit = i + k;
                    if (limit > words.size()) {
                        limit = words.size();
                    }
                    String wordComb = word1;
                    for (int j = i + 1; j < limit; j++) {
                        String word2 = words.get(j).trim();
                        if (!stopWords.contains(word2.toLowerCase())) {
                            wordComb +=  " " + word2;
                            if (!wordCombs.contains(wordComb)) {
                                wordCombs.add(wordComb);
                            }
                        }
                    }
                }
            }
        }
        return wordCombs;
    }

    public List<String> getAllCases(List<String> words, List<String> stopWords) {
        List<String> queryCases = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i).toString().trim();
            if (!stopWords.contains(word.toLowerCase())) {
                // Add original word
                queryCases.add(word);
                // Add all-uppercase word (if not exists)
                if (!word.toUpperCase().equals(word)) {
                    queryCases.add(word.toUpperCase());
                }
                // Add all-lowercase word (if not exists)
                if (!word.toLowerCase().equals(word)) {
                    queryCases.add(word.toLowerCase());
                }
                // Add first letter(s) uppercase word(s)
                String[] wordSplit = word.split("\\s+");
                // Split word before-hand in case of a word combination
                if (wordSplit.length > 1) {
                    StringBuilder nameCaseComb = new StringBuilder();
                    for (String s : wordSplit) {
                        String wordFromSplit = s.trim();
                        String nameCaseWord = wordFromSplit.substring(0, 1).toUpperCase() + wordFromSplit.substring(1);
                        if (!nameCaseComb.toString().equals("")) {
                            nameCaseComb.append(" ");
                        }
                        nameCaseComb.append(nameCaseWord);
                    }
                    queryCases.add(nameCaseComb.toString());
                } else {
                    String nameCaseWord = word.substring(0, 1).toUpperCase() + word.substring(1);
                    queryCases.add(nameCaseWord);
                }
            }
        }
        return queryCases;
    }

    public void searchWiki(final List<String> titles) throws UnsupportedEncodingException {
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        int titlesCount = 0;
        List<String> queries = new ArrayList<>();
        StringBuilder queryString = new StringBuilder();

        for (int i = 0; i < titles.size(); i++) {
            if (!queryString.toString().equals("")) {
                queryString.append("|");
            }
            if (titlesCount > 30) {
                queries.add(queryString.toString());
                titlesCount = 0;
                queryString = new StringBuilder();
            }
            queryString.append(titles.get(i));
            titlesCount++;
            if (i == titles.size() - 1) {
                queries.add(queryString.toString());
            }
        }

        // Start the queue and collect request query strings
        requestQueue.start();

        for (int i = 0; i < queries.size(); i++) {
            String query = URLEncoder.encode(queries.get(i),"utf-8");

            // Define url for request sent to Wikipedia
            String url = "https://en.wikipedia.org/w/api.php?" + // English Wikipedia API
                         "action=query" +                        // Action is to fetch data
                         "&titles=" + query +                    // Query list of words given
                         "&format=json";                         // Output format is of type json
            JsonObjectRequest request = getRequest(url, queries.size());

            // Add the request to the RequestQueue.
            requestQueue.add(request);
        }
    }

    private JsonObjectRequest getRequest (String url, final int requestNumber) {
        return new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Update request count
                    requestCount++;
                    JSONObject pages = response.getJSONObject("query").getJSONObject("pages");
                    Iterator <?> pageKeys = pages.keys();
                    while (pageKeys.hasNext()) {
                        String key = (String) pageKeys.next();
                        if (pages.get(key) instanceof JSONObject) {
                            JSONObject pageEntry = (JSONObject) pages.get(key);
                            String title = pageEntry.get("title").toString().toLowerCase();
                            if (!key.startsWith("-")) {
                                String link = "https://en.wikipedia.org/?curid=" + key;
                                // Add word to list if not yet found previously
                                if (!wordsFound.contains(title.toLowerCase())) {
                                    wordLinksMap.put(title, link);
                                    wordsFound.add(title.toLowerCase());
                                }
                            }
                        }
                    }
                    // If all requests are done, add links to keywords
                    if (requestNumber == requestCount) {
                        new ReplaceInText().execute(wordsFound);
                    }
                } catch (JSONException e) {
                    // Otherwise fail and return error stack
                    e.printStackTrace();
                }
            }}, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Wiki list error", error);
            }
        });
    }

    private class ReplaceInText extends AsyncTask<List, Void, String> {
        @Override
        protected String doInBackground(List... params) {
            // From words found, get their indices in text
            // and track words to ignore (if their combination
            // also has a link associated)
            String userTextWiki = getIntent().getStringExtra("userText");
            LinkedHashMap <String, Integer> wordIndexMap = new LinkedHashMap<>();
            List<String> wordsToRemove = new ArrayList<>();

            for (int i = 0; i < wordsFound.size(); i++) {
                String word = wordsFound.get(i);
                String[] splitWord = word.split("\\s+");
                if (splitWord.length > 1) {
                    // Check if word combination exists in the user text
                    // (i.e. as consecutive words)
                    assert userTextWiki != null;
                    if (userTextWiki.toLowerCase().contains(word)) {
                        Collections.addAll(wordsToRemove, splitWord);
                    } else {
                        // Otherwise, remove the combination and keep
                        // individual words instead
                        wordsToRemove.add(word);
                    }
                }
                assert userTextWiki != null;
                int index = userTextWiki.toLowerCase().indexOf(word);
                wordIndexMap.put(word, index);
            }

            // Now, remove those words from the index map created
            wordIndexMap.keySet().removeAll(wordsToRemove);

            // Sort map by index
            HashMap<String, Integer> wordIndexMapSorted = sortByValues(wordIndexMap);

            // Finally, loop over remaining word map and insert links into text
            // Keep track of number of extra characters added to the text by adding
            // in the links
            int charsToAdd = 0;
            for (Object wordObject : wordIndexMapSorted.keySet()) {
                String word = wordObject.toString().trim();
                int wordIndex = wordIndexMap.get(word) + charsToAdd;
                // Show word in its original capitalization
                assert userTextWiki != null;
                String wordOriginal = userTextWiki.substring(wordIndex, wordIndex + word.length());
                // Prepare link to replace word by
                String link = "<a href=\"" + wordLinksMap.get(word) + "\">" + wordOriginal + "</a>";
                // Build up userTextWiki piece by piece
                userTextWiki = userTextWiki.substring(0, wordIndex) + link +
                                   userTextWiki.substring(wordIndex + word.length());
                // Update word indices to account for added link chars
                charsToAdd += link.length() - word.length();
            }
            return userTextWiki;
        }

        // Note, that the sortByValues function is referenced and not our own implementation!
        // Its reference stems from a Java collections tutorial. Source can be found at:
        // https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/
        private HashMap<String, Integer> sortByValues(HashMap<String, Integer> map) {
            List<Map.Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
            Collections.sort(list, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Comparable) ((Map.Entry) (o1)).getValue())
                            .compareTo(((Map.Entry) (o2)).getValue());
                }
            });
            HashMap<String, Integer> sortedHashMap = new LinkedHashMap<String, Integer>();
            for (Map.Entry<String, Integer> entry : list) {
                sortedHashMap.put(entry.getKey(), entry.getValue());
            }
            return sortedHashMap;
        }

        @Override
        protected void onPostExecute(String userTextWiki) {
            // Display text with links once augmented
            TextView wikiTextView = findViewById(R.id.text_from_wiki);
            wikiTextView.setMovementMethod(LinkMovementMethod.getInstance());
            wikiTextView.setText(Html.fromHtml(userTextWiki));
        }
    }

    @Override
    public void onBackPressed()
    {
        // Clear all collated lists and hash maps as the user returns to
        // the input screen to send off another query
        super.onBackPressed();
        wordsFound.clear();
        wordLinksMap.clear();
    }

}
