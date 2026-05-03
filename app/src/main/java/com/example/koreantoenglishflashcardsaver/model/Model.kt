package com.example.koreantoenglishflashcardsaver.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

@Entity(tableName = "flashcards")
data class Flashcard(
    var word: String = "",
    var translations: MutableList<Pair<String, Array<String>>>? = null,
    var examples: MutableList<Pair<String, String>>? = null,
    var directTranslation: String? = null) : Serializable{
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    /**
     * Returns the translations in a readable format as a single String
     */
    fun getTranslationsAsString(): String? {
        val combinedTranslations: MutableList<String> = mutableListOf()
        if (translations != null) {
            for (translation in translations!!) {
                combinedTranslations.add(
                    translation.second.joinToString(
                        separator = ",",
                        prefix = "${translation.first}: "
                    )
                )
            }
            return combinedTranslations.joinToString(separator = "\n")
        }
        else return null
    }

    /**
     * Returns the example sentences in a readable format as a single String.
     * If there are none, returns null
     */
    fun getExamplesAsString(): String?{
        val combinedExamples: MutableList<String> = mutableListOf()
        if (examples != null) {
            for (example in examples!!){
                combinedExamples.add("${example.first}\n${example.second}")
            }
            return combinedExamples.joinToString(separator = "\n")
        }
        else return null
    }

    /**
     * Returns the example sentences results in an array format to be consistent with translations
     * for some functions.
     */
    fun getExamplesAsArray():MutableList<Pair<String, Array<String>>>?{
        return examples?.map { pair ->
            // Keep the first string, wrap the second string in an array
            pair.first to arrayOf(pair.second)
        }?.toMutableList()
    }

    /**
     * Returns the direct translation results in an array format to be consistent with translations
     * for some functions.
     */
    fun getDirectTranslationAsArray():MutableList<Pair<String, Array<String>>>?{
        return if(directTranslation == null) null else mutableListOf(Pair(word, arrayOf(directTranslation!!)))
    }
}
class Converters {
    private val gson = Gson()
    @TypeConverter
    fun getTranslationsfromString(translations: String?): MutableList<Pair<String, Array<String>>>? {
        val listType = object : TypeToken<MutableList<Pair<String, Array<String>>>>() {}.type
        return gson.fromJson(translations, listType)
    }

    @TypeConverter
    fun getTranslationsAsString(translations: MutableList<Pair<String, Array<String>>>?): String? {
        return gson.toJson(translations)

    }

    @TypeConverter
    fun getExamplesFromString(examples: String): MutableList<Pair<String, String>>?{
        val listType = object : TypeToken<MutableList<Pair<String, Array<String>>>>() {}.type
        return gson.fromJson(examples, listType)
    }
    @TypeConverter
    fun getExamplesAsString(examples: MutableList<Pair<String, String>>?): String?{
        return gson.toJson(examples)
    }
}
@Entity(tableName = "decks")
data class Deck(
    var deckName: String,
    var selected: Boolean) : Serializable{
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}
