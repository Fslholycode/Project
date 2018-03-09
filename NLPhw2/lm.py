#!/bin/python

from __future__ import division
from __future__ import print_function
from __future__ import absolute_import

import collections
from math import log
import sys

# Python 3 backwards compatibility tricks
if sys.version_info.major > 2:

    def xrange(*args, **kwargs):
        return iter(range(*args, **kwargs))

    def unicode(*args, **kwargs):
        return str(*args, **kwargs)

class LangModel:
    def fit_corpus(self, corpus):
        """Learn the language model for the whole corpus.
        The corpus consists of a list of sentences."""
        for s in corpus:
            self.fit_sentence(s)
        self.norm()

    def perplexity(self, corpus):
        """Computes the perplexity of the corpus by the model.
        Assumes the model uses an EOS symbol at the end of each sentence.
        """
        return pow(2.0, self.entropy(corpus))

    def entropy(self, corpus):
        num_words = 0.0
        sum_logprob = 0.0
        for s in corpus:
            num_words += len(s) + 1 # for EOS
            sum_logprob += self.logprob_sentence(s)
        return -(1.0/num_words)*(sum_logprob)

    def logprob_sentence(self, sentence):
        p = 0.0
        for i in xrange(len(sentence)):
            if i == 0:
                p += self.cond_logprob("START_OF_SENTENCE", "START_OF_SENTENCE", sentence[0])
            elif i == 1:
                p += self.cond_logprob("START_OF_SENTENCE", sentence[0], sentence[1])
            else:
                p += self.cond_logprob(sentence[i-2],sentence[i-1], sentence[i])
        p += self.cond_logprob(sentence[len(sentence)-2], sentence[len(sentence)-1], "END_OF_SENTENCE")
        p += self.cond_logprob(sentence[len(sentence)-1], "END_OF_SENTENCE", "END_OF_SENTENCE")
        return p

    # required, update the model when a sentence is observed
    def fit_sentence(self, sentence): pass
    # optional, if there are any post-training steps (such as normalizing probabilities)
    def norm(self): pass
    # required, return the log2 of the conditional prob of word, given previous words
    def cond_logprob(self, previous2, previous1, word): pass
    # required, the list of words the language model suports (including EOS)
    def vocab(self): pass

# class Trigram(LangModel):
#     def __init__(self, backoff = 0.000001):
#         self.model = dict()
#         self.lbackoff = log(backoff, 2)
#
#     def inc_word(self, w):
#         if w in self.model:
#             self.model[w] += 1.0
#         else:
#             self.model[w] = 1.0
#
#     def fit_sentence(self, sentence):
#         for i in range(len(sentence)):
#             if i == 0:
#                 self.inc_word("START_OF_SENTENCE"+" "+"START_OF_SENTENCE")
#                 self.inc_word("START_OF_SENTENCE"+" "+"START_OF_SENTENCE"+" "+sentence[0])
#             elif i == 1:
#                 self.inc_word("START_OF_SENTENCE"+" "+sentence[0])
#                 self.inc_word("START_OF_SENTENCE"+" "+sentence[0]+" "+sentence[1])
#             elif i > 2:
#                 self.inc_word(sentence[i-2]+" "+sentence[i-1]+" "+sentence[i])
#                 self.inc_word(sentence[i-2]+" "+sentence[i-1])
#         self.inc_word(sentence[len(sentence)-2]+" "+sentence[len(sentence)-1])
#         self.inc_word(sentence[len(sentence)-2]+" "+sentence[len(sentence)-1]+" "+"END_OF_SENTENCE")
#         self.inc_word(sentence[len(sentence)-1]+" "+"END_OF_SENTENCE")
#         self.inc_word(sentence[len(sentence)-1]+" "+"END_OF_SENTENCE"+" "+"END_OF_SENTENCE")
#
#     def norm(self):
#         """Normalize and convert to log2-probs."""
#         for list in self.model:
#             splitlist = list.split(" ")
#             if len(splitlist) == 2:
#                 for longlist in self.model:
#                     splitlonglist = longlist.split(" ")
#                     if len(splitlonglist) == 3 and splitlonglist[0] == splitlist[0] and splitlonglist[1] == splitlist[1]:
#                         self.model[longlist] = log(self.model[longlist], 2) - log(self.model[list], 2)
#
#     def cond_logprob(self, previous2, previous1, word):
#         list = ""
#         list += previous2
#         list += previous1
#         list += word
#         if list in self.model:
#             return self.model[list]
#         else:
#             return self.lbackoff
#
#     def vocab(self):
#         res = []
#         for s in self.model.keys():
#             x = s.split(" ")
#             if len(x) == 3:
#                 res.append(x[2])
#         return res


class Trigram(LangModel):
    def __init__(self, backoff = 0.001):
        self.model = dict()
        self.word = dict()
        self.lbackoff = log(backoff,2)
        self.k = 0.001

    def inc_word(self, prev, w):
        if prev in self.model:
            if w in self.model[prev]:
                self.model[prev][w] += 1.0
            else:
                self.model[prev][w] = 1.0
        else:
            self.model[prev] = dict()
            self.model[prev][w] = 1.0;

    def in_word(self, w):
        if w not in self.word:
            self.word[w] = 1.0

    def fit_sentence(self, sentence):
        for i in range(len(sentence)):
            if i == 0:
                self.inc_word("START_OF_SENTENCE"+" "+"START_OF_SENTENCE", sentence[0])
                self.in_word(sentence[0])
            elif i == 1:
                self.inc_word("START_OF_SENTENCE"+" "+sentence[0], sentence[1])
                self.in_word(sentence[1])
            elif i > 2:
                self.inc_word(sentence[i-2]+" "+sentence[i-1], sentence[i])
                self.in_word(sentence[i])
        self.inc_word(sentence[len(sentence)-2]+" "+sentence[len(sentence)-1], "END_OF_SENTENCE")
        self.inc_word(sentence[len(sentence)-1]+" "+"END_OF_SENTENCE", "END_OF_SENTENCE")
        self.in_word("END_OF_SENTENCE")
    #
    # def smoothing(self):
    #     for w in self.word:
    #         for prev in self.model:
    #             if w not in self.model.keys():
    #                 self.model[prev][w] = 0.75
    #             else:
    #                 self.model[prev][w] += 0.75

    def norm(self):
        """Normalize and convert to log2-probs."""
        for prev in self.model:
            tot = 0.0
            for w in self.model[prev]:
                tot += self.model[prev][w]
            self.model[prev]["ssssssum"] = tot

    def cond_logprob(self, previous2, previous1, word):
        list = ""
        list += previous2
        list += " "
        list += previous1
        if list in self.model:
            if word in self.model[list]:
                return log(self.model[list][word], 2) - log(self.model[list]["ssssssum"], 2)
                # return log((self.model[list][word]+self.k), 2)-log(self.model[list]["ssssssum"]+len(self.word)*self.k, 2)
            else:
                # return log(self.k, 2) - log(len(self.word)*self.k+self.model[list]["ssssssum"], 2)
                return self.lbackoff
        else:
            return self.lbackoff

    def vocab(self):
        return self.word.keys()