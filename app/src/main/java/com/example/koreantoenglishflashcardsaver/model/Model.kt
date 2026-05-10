package com.example.koreantoenglishflashcardsaver.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var word: String = "",
    var translations: MutableList<TranslationEntry>? = null,
    var examples: MutableList<ExampleEntry>? = null,
    var directTranslation: String? = null) : Parcelable{


    /**
     * Returns the translations in a readable format as a single String
     */
    fun getTranslationsAsString(): String? {
        val combinedTranslations: MutableList<String> = mutableListOf()
        if (translations != null) {
            for (translation in translations!!) {
                combinedTranslations.add(
                    translation.translationBody.joinToString(
                        separator = ",",
                        prefix = "${translation.word}: "
                    )
                )
            }
            return combinedTranslations.joinToString(separator = "\n")
        }
        else return null
    }

    /**
     * Returns the example sentences results in an array format to be consistent with translations
     * for some functions.
     */
    fun getTranslationsAsArray():MutableList<Pair<String, List<String>>>?{
        return translations?.map { pair ->
            // Keep the first string, wrap the second string in an array
            pair.word to pair.translationBody
        }?.toMutableList()
    }

    /**
     * Returns the example sentences in a readable format as a single String.
     * If there are none, returns null
     */
    fun getExamplesAsString(): String?{
        val combinedExamples: MutableList<String> = mutableListOf()
        if (examples != null) {
            for (example in examples!!){
                combinedExamples.add("${example.word}\n${example.exampleBody}")
            }
            return combinedExamples.joinToString(separator = "\n")
        }
        else return null
    }

    /**
     * Returns the example sentences results in an array format to be consistent with translations
     * for some functions.
     */
    fun getExamplesAsArray():MutableList<Pair<String, List<String>>>?{
        return examples?.map { pair ->
            // Keep the first string, wrap the second string in an array
            pair.word to listOf(pair.exampleBody)
        }?.toMutableList()
    }

    /**
     * Returns the direct translation results in an array format to be consistent with translations
     * for some functions.
     */
    fun getDirectTranslationAsArray():MutableList<Pair<String, List<String>>>?{
        return if(directTranslation == null) null else mutableListOf(Pair(word, listOf(directTranslation!!)))
    }

    /**
     * Copies just the fields and not the id of the provided flashcard
     */
    fun copyMembers(flashcard: Flashcard){
        word = flashcard.word
        translations = flashcard.translations
        examples = flashcard.examples
        directTranslation = flashcard.directTranslation
    }
}
class Converters {
    private val gson = Gson()
    @TypeConverter
    fun getTranslationsfromString(translations: String?): MutableList<TranslationEntry>? {
        val listType = object : TypeToken<MutableList<TranslationEntry>>() {}.type
        return gson.fromJson(translations, listType)
    }

    @TypeConverter
    fun getTranslationsAsString(translations: MutableList<TranslationEntry>?): String? {
        return gson.toJson(translations)

    }

    @TypeConverter
    fun getExamplesFromString(examples: String): MutableList<ExampleEntry>?{
        val listType = object : TypeToken<MutableList<ExampleEntry>>() {}.type
        return gson.fromJson(examples, listType)
    }
    @TypeConverter
    fun getExamplesAsString(examples: MutableList<ExampleEntry>?): String?{
        return gson.toJson(examples)
    }
}
@Parcelize
data class TranslationEntry(val word: String, val translationBody: List<String>) : Parcelable
@Parcelize
data class ExampleEntry(val word: String, val exampleBody: String) : Parcelable
@Entity(tableName = "decks")
data class Deck(
    var deckName: String,
    var selected: Boolean) : Serializable{
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}
