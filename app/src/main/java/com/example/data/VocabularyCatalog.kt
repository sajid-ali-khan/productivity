package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

object VocabularyCatalog {

    data class CuratedWord(
        val word: String,
        val phonetic: String,
        val partOfSpeech: String,
        val definition: String,
        val example: String,
        val speakingTip: String,
        val sampleDialogue: String
    )

    // Curated catalog designed specifically to build speaking confidence and natural phrasing
    val CURATED_WORDS = listOf(
        CuratedWord(
            word = "Articulate",
            phonetic = "/ɑːrˈtɪk.jə.leɪt/",
            partOfSpeech = "adjective / verb",
            definition = "Expressing ideas clearly, coherently, and effortlessly in speech.",
            example = "She was praised for her articulate presentation during the team meeting.",
            speakingTip = "Use when praising someone's clear speech: 'You articulated that point really well.'",
            sampleDialogue = "A: Did the client grasp our proposal?\nB: Yes, she articulated our milestones so clearly that they approved right away."
        ),
        CuratedWord(
            word = "Succinct",
            phonetic = "/səkˈsɪŋkt/",
            partOfSpeech = "adjective",
            definition = "Briefly and clearly expressed; getting directly to the point without waffle.",
            example = "Keep your project update succinct so everyone stays engaged.",
            speakingTip = "Use instead of 'short and sweet': 'Let's keep this recap succinct.'",
            sampleDialogue = "A: How long should my status update be?\nB: Keep it succinct—just two minutes covering key wins."
        ),
        CuratedWord(
            word = "Pragmatic",
            phonetic = "/præɡˈmæt.ɪk/",
            partOfSpeech = "adjective",
            definition = "Dealing with things sensibly and realistically based on practical conditions rather than theory.",
            example = "We need a pragmatic roadmap that works with our current budget.",
            speakingTip = "Use when grounding a discussion: 'Let's take a pragmatic approach here.'",
            sampleDialogue = "A: Should we redesign everything from scratch?\nB: Let's be pragmatic and fix the highest priority bottlenecks first."
        ),
        CuratedWord(
            word = "Lucid",
            phonetic = "/ˈluː.sɪd/",
            partOfSpeech = "adjective",
            definition = "Expressed clearly; easy to follow and comprehend.",
            example = "His lucid explanation made complex algorithms feel intuitive.",
            speakingTip = "Use when acknowledging a good breakdown: 'Thanks, that was a very lucid summary.'",
            sampleDialogue = "A: Was the lecture confusing?\nB: Not at all, the professor gave a remarkably lucid breakdown."
        ),
        CuratedWord(
            word = "Candid",
            phonetic = "/ˈkæn.dɪd/",
            partOfSpeech = "adjective",
            definition = "Truthful and straightforward; frank and genuine without pretense.",
            example = "I appreciated her candid feedback on my draft speech.",
            speakingTip = "Great opening phrase: 'If I can be completely candid with you...'",
            sampleDialogue = "A: What do you really think of my presentation?\nB: If I may be candid, your opening was great, but the conclusion was rushed."
        ),
        CuratedWord(
            word = "Resilient",
            phonetic = "/rɪˈzɪl.jənt/",
            partOfSpeech = "adjective",
            definition = "Able to withstand or recover quickly from difficult conditions or setbacks.",
            example = "The team remained resilient despite multiple launch delays.",
            speakingTip = "Use when acknowledging grit: 'I admire how resilient your team has been.'",
            sampleDialogue = "A: Didn't you lose all your data yesterday?\nB: Yes, but we stayed resilient and rebuilt our core plan by morning."
        ),
        CuratedWord(
            word = "Nuanced",
            phonetic = "/ˈnjuː.ɑːnst/",
            partOfSpeech = "adjective",
            definition = "Characterized by subtle distinctions in meaning, expression, or tone.",
            example = "This problem is nuanced and requires more than a simple yes-or-no answer.",
            speakingTip = "Use to elevate debate: 'That's a valid view, but the reality is more nuanced.'",
            sampleDialogue = "A: So is remote work good or bad?\nB: It's nuanced—it boosts focus for many, but demands deliberate communication."
        ),
        CuratedWord(
            word = "Eloquent",
            phonetic = "/ˈel.ə.kwənt/",
            partOfSpeech = "adjective",
            definition = "Fluent or persuasive in speaking; movingly expressive.",
            example = "He delivered an eloquent toast that touched everyone in the room.",
            speakingTip = "Compliment someone's storytelling: 'You put that in such an eloquent way.'",
            sampleDialogue = "A: How did the keynote go?\nB: The speaker was remarkably eloquent; the whole audience was captivated."
        ),
        CuratedWord(
            word = "Pivotal",
            phonetic = "/ˈpɪv.ə.təl/",
            partOfSpeech = "adjective",
            definition = "Of crucial importance in relation to the development or outcome of something.",
            example = "Securing this partnership was pivotal for our company's growth.",
            speakingTip = "Use instead of 'very important': 'This is a pivotal moment for our goals.'",
            sampleDialogue = "A: Did that conversation make a difference?\nB: Absolutely, it was pivotal in turning the whole project around."
        ),
        CuratedWord(
            word = "Astute",
            phonetic = "/əˈstjuːt/",
            partOfSpeech = "adjective",
            definition = "Having or showing an ability to accurately assess situations and turn this to one's advantage.",
            example = "An astute observation from Maya saved us hours of wasted effort.",
            speakingTip = "Use in discussions: 'That's an astute observation.'",
            sampleDialogue = "A: I noticed the numbers in Q2 don't align with our projection.\nB: That's very astute of you—let's examine that discrepancy right away."
        ),
        CuratedWord(
            word = "Empathetic",
            phonetic = "/ˌem.pəˈθet.ɪk/",
            partOfSpeech = "adjective",
            definition = "Showing an ability to understand and sincerely share the feelings of another.",
            example = "Great leaders are deeply empathetic listeners.",
            speakingTip = "Use in conversation: 'I want to be empathetic to what you're dealing with.'",
            sampleDialogue = "A: Our customer service scores jumped this month.\nB: That's because our reps took a much more empathetic approach with callers."
        ),
        CuratedWord(
            word = "Tenacious",
            phonetic = "/təˈneɪ.ʃəs/",
            partOfSpeech = "adjective",
            definition = "Tending to keep a firm hold of something; persistent and determined.",
            example = "Her tenacious pursuit of the truth uncovered the whole story.",
            speakingTip = "Use to commend persistence: 'Your tenacious follow-up got this done.'",
            sampleDialogue = "A: Did you finally get the visa approved?\nB: Yes! It took six months, but being tenacious paid off."
        ),
        CuratedWord(
            word = "Diplomatic",
            phonetic = "/ˌdɪp.ləˈmæt.ɪk/",
            partOfSpeech = "adjective",
            definition = "Having or showing an ability to handle sensitive matters with tact and grace.",
            example = "He found a diplomatic way to disagree without offending anyone.",
            speakingTip = "Use when mediating: 'Let's find a diplomatic way to word this request.'",
            sampleDialogue = "A: How do I tell my manager their deadline is unrealistic?\nB: Frame it diplomatically around resource constraints rather than complaints."
        ),
        CuratedWord(
            word = "Conscientious",
            phonetic = "/ˌkɒn.ʃiˈen.ʃəs/",
            partOfSpeech = "adjective",
            definition = "Wishing to do one's work or duty thoroughly and diligently.",
            example = "He is a conscientious engineer who never cuts corners on quality.",
            speakingTip = "Use in recommendations: 'She is exceptionally conscientious in her work.'",
            sampleDialogue = "A: Why do you trust Leo with critical accounts?\nB: Because he is remarkably conscientious; nothing ever falls through the cracks."
        ),
        CuratedWord(
            word = "Compelling",
            phonetic = "/kəmˈpel.ɪŋ/",
            partOfSpeech = "adjective",
            definition = "Evoking interest, attention, or admiration in a powerfully irresistible way.",
            example = "The speaker built a compelling case for adopting sustainable habits.",
            speakingTip = "Use instead of 'really interesting': 'You made a compelling argument.'",
            sampleDialogue = "A: Did they buy into the pitch?\nB: Yes, the data visualization made the problem truly compelling."
        ),
        CuratedWord(
            word = "Equanimity",
            phonetic = "/ˌek.wəˈnɪm.ə.ti/",
            partOfSpeech = "noun",
            definition = "Mental calmness, composure, and evenness of temper, especially in a difficult situation.",
            example = "She responded to the harsh criticism with remarkable equanimity.",
            speakingTip = "Use to describe composure: 'Maintaining equanimity under pressure is a superpower.'",
            sampleDialogue = "A: Were you nervous when the demo failed on stage?\nB: I was inside, but keeping my equanimity helped me pivot quickly."
        ),
        CuratedWord(
            word = "Discerning",
            phonetic = "/dɪˈsɜː.nɪŋ/",
            partOfSpeech = "adjective",
            definition = "Having or showing good judgment and acute perception.",
            example = "A discerning reader will notice the subtle motifs woven throughout the text.",
            speakingTip = "Use to compliment taste: 'You have a very discerning eye for design.'",
            sampleDialogue = "A: Which candidate stood out to you?\nB: Elena—her discerning answers showed deep industry awareness."
        ),
        CuratedWord(
            word = "Incisive",
            phonetic = "/ɪnˈsaɪ.sɪv/",
            partOfSpeech = "adjective",
            definition = "Intelligently analytical and clear-thinking; cutting straight to the heart of an issue.",
            example = "Her incisive questions forced us to rethink our assumptions.",
            speakingTip = "Use when someone asks a brilliant question: 'That was an incisive question.'",
            sampleDialogue = "A: How did the Q&A session go?\nB: The board asked very incisive questions that sharpened our strategy."
        )
    )

    fun getWordForDate(dateStr: String): CuratedWord {
        // Deterministic date-based mapping using hash or day of year
        val hash = dateStr.hashCode()
        val index = (Math.abs(hash)) % CURATED_WORDS.size
        return CURATED_WORDS[index]
    }

    /**
     * Attempts to fetch audio pronunciation URL and enrich definitions from the Free Dictionary API.
     */
    suspend fun fetchApiEnrichment(word: String): String? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.dictionaryapi.dev/api/v2/entries/en/${word.lowercase()}"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    val firstEntry = jsonArray.getJSONObject(0)
                    if (firstEntry.has("phonetics")) {
                        val phonetics = firstEntry.getJSONArray("phonetics")
                        for (i in 0 until phonetics.length()) {
                            val p = phonetics.getJSONObject(i)
                            if (p.has("audio") && p.getString("audio").isNotBlank()) {
                                var audio = p.getString("audio")
                                if (audio.startsWith("//")) {
                                    audio = "https:$audio"
                                }
                                return@withContext audio
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("VocabularyCatalog", "Dictionary API enrichment skipped: ${e.message}")
        }
        null
    }
}
