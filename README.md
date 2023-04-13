# KIWi App

*...Have you ever found yourself binge-reading Wikipedia, while you've already found the answer to what you were looking for originally? But new articles just continue to catch your interest?*

Then this app is for you! Now quick and directed Wikipedia look-ups are at your fingertips, letting you skim through more topics in less time.

**KIWi** stands for **K**eyword **I**nterface **Wi**ki and much like the name suggests, adds Wikipedia links to your texts! By providing a string of words or text of interest, the same text will be returned to you with the corresponing Wikipedia links embedded. 

# How it works

When your text is submitted, it is first broken up into a list of words. Just like in natural language processing, any stop words (from [Ref. [1]](https://www.textfixer.com/tutorials/common-english-words.txt)) are excluded from this list initially.

Next, look for word combinations that we might also try (see `getAllCombs`). Note that we have written the function to handle all consecutive word combinations in that sentence. For practicality, however, we have chosen **`n=3`**, where **`n`** are the number of consecutive words as most names, places, etc. should not surpass this length.

Now that we have the list of words and their combinations, it is important to now that the word's capitalization also matters. For this step, `getAllCases` will collect several capitalization forms of each word (or word combination), namely: all-uppercase, all-lowercase, and a name-case which only capitalizes the first letter of words and of words in word combinations.

After we have taken care of word-combinations and -cases, all that is left to do is collect them (with **`|`** operators) and to send the queries to the Wikipedia API, see `searchWiki`. We limit ourselves to a *title*-only search, not full text arcitles. As there is a size-limit of the number of search terms that can be sent, we will separate the queries (in word chunks of 30) and add them to the request queue.

As each response is being analyzed (see `getRequest`), we collect the words found and their Wikipedia links until the last request reponse has been received. Note, that there are many ways to insert the collected links into the user's text but we have chosen the following as it demonstrated to be the most safe and robust, though it requires some preprocessing. In `ReplaceInText`, we first collect a hashmap with their respective indices in the user's text and also any words to remove (word combinations 
that are not actually consecutive in our text due to the initial removal of stop words). We remove these words and resort as a `LinkedHashMap` according to its (ascending) index (see [Ref. [2]](https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/)). Now, all that is left to do is to loop through this sorted hashmap and build up the user string (with links). We can achieve this by taking the beginning part of the user's text until the index is reached, add the embedded link text and then add the remainder of the user's text. After each iteration, we add on the extra number of characters that are now in the string due to the added link and we continue this cycle until the last word/link has been embedded.

# References

 1. The list of stop words to exclude from the text stem from the *common english stop words* from the **TEXTFIXER** tutorials page, found [here](https://www.textfixer.com/tutorials/common-english-words.txt).

2. We have included a `sortByValues` function which is not our own implementation! Its reference stems from a *Java collections tutorial* (see [sortByValues](https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/)).
