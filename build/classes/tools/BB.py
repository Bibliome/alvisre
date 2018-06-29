#!/usr/bin/env python


# Copyright (c) 2013, Institut National de la Recherche Agronomique (INRA)
# All rights reserved.

# Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

#     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#     Neither the names of the Institut National de la Recherche Agronomique (INRA) and BioNLP-ST 2013 nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

from obo import Ontology, OntologyReader, UnhandledTagFail, DanglingReferenceFail, DeprecatedTagSilent
from bionlpst import *
from StringIO import StringIO

class Score(object):
    def __init__(self):
        object.__init__(self)

    def score(self, doc1, doc2, a1, a2):
        raise NotImplemented()

    def is_best(self, score1, score2):
        raise NotImplemented()

    def is_something(self, score):
        raise NotImplemented()

    def _best_match(self, doc1, doc2, a1, set2):
        best = None
        best_score = None
        for a2 in set2:
            score = self.score(doc1, doc2, a1, a2)
            if self.is_something(score) and (best is None or self.is_best(score, best_score)):
                best = a2
                best_score = score
        return best, best_score

    def pairing(self, doc1, doc2, set1, set2):
        return dict((a1, self._best_match(doc1, doc2, a1, set2)) for a1 in set1)


class NumericScore(Score):
    def __init__(self, threshold=0, is_cost=False):
        Score.__init__(self)
        self.threshold = threshold
        self.is_cost = is_cost

    def is_best(self, score1, score2):
        if self.is_cost:
            return score1 < score2
        return score1 > score2

    def is_something(self, score):
        if self.is_cost:
            return score < self.threshold
        return score > self.threshold


class Jaccard(NumericScore):
    def __iter__(self):
        NumericScore.__iter__(self)

    def score(self, doc1, doc2, a1, a2):
        inter = self._tb_inter(a1.boundaries, a2.boundaries)
        union = len(a1) + len(a2) - inter
        return float(inter) / union

    def _tb_inter(self, bs1, bs2):
        r = 0
        i1 = bs1.__iter__()
        i2 = bs2.__iter__()
        try:
            b1 = i1.next()
            b2 = i2.next()
            while True:
                r += self._inter(b1, b2)
                if b1[1] >= b2[1]:
                    b2 = i2.next()
                if b1[1] <= b2[1]:
                    b1 = i1.next()
        except StopIteration:
            return r

    def _inter(self, b1, b2):
        if b1[0] >= b2[1]:
            return 0
        if b2[0] >= b1[1]:
            return 0
        return min(b1[1], b2[1]) - max(b1[0], b2[0])


class CompoundNumericScore(NumericScore):
    def __init__(self, scores, threshold=0):
        if len(scores) == 0:
            raise Exception()
        is_cost = None
        for s in scores.itervalues():
            if not isinstance(s, NumericScore):
                raise Exception(str(s) + ' is not a NumericScore instance')
            if is_cost is None:
                is_cost = s.is_cost
            elif s.is_cost != is_cost:
                raise Exception()
        NumericScore.__init__(self, threshold, is_cost)
        self.scores = dict(scores)

    def is_something(self, score):
        glob, comp = score
        for name, v in comp.iteritems():
            if not self.scores[name].is_something(v):
                return False
        return NumericScore.is_something(self, glob)

    def is_best(self, score1, score2):
        glob1, comp1 = score1
        glob2, comp2 = score2
        return NumericScore.is_best(self, glob1, glob2)

    def score(self, doc1, doc2, a1, a2):
        comp = dict([(name, s.score(doc1, doc2, a1, a2)) for (name, s) in self.scores.iteritems()])
        glob = self.aggregate(comp)
        return glob, comp

    def aggregate(self, comp):
        raise NotImplemented()


class MultiplicativeNumericScore(CompoundNumericScore):
    def __init__(self, scores, threshold=0):
        CompoundNumericScore.__init__(self, scores, threshold)

    def aggregate(self, comp):
        r = 1
        for v in comp.itervalues():
            r *= v
        return r


class AdditiveNumericScore(CompoundNumericScore):
    def __init__(self, scores, threshold=0):
        CompoundNumericScore.__init__(scores, threshold)

    def aggregate(self, comp):
        r = 1
        for v in comp.itervalues():
            r += v
        return r


class Wang_Normalization(NumericScore, dict):
    def __init__(self, ontology, weight):
        NumericScore.__init__(self)
        dict.__init__(self, ((term, self._get_s_values(term)) for term in ontology.iterterms()))
        self.ontology = ontology
        self.weight = weight
        
    def _ancestors(self, term, depth):
        yield term, depth
        if 'is_a' in term.references:
            for r in term.references['is_a']:
                for p in self._ancestors(r.reference_object, depth + 1):
                    yield p

    def _get_s_values(self, term):
        result = {}
        for ancestor, depth in self._ancestors(term, 0):
            if ancestor in result:
                result[ancestor] = min(result[ancestor], depth)
            else:
                result[ancestor] = depth
        return result

    def value(self, term):
        if term not in self:
            return 0
        return sum((self.weight ** depth) for depth in self[term].itervalues())

    def s_values(self, term):
        if term not in self:
            return {}
        return dict((t, (self.weight ** d)) for t, d in self[term].iteritems())

    def term_similarity(self, term1, term2):
        if term1 not in self:
            return 0
        if term2 not in self:
            return 0
        if term1 == term2:
            return 1.0
        v1 = self.value(term1)
        v2 = self.value(term2)
        sv1 = self.s_values(term1)
        sv2 = self.s_values(term2)
        inter = set(sv1) & set(sv2)
        return sum((sv1[t] + sv2[t]) for t in inter) / (v1 + v2)

    def score(self, doc1, doc2, a1, a2):
        term1 = self.ontology.stanzas[a1.referent]
        term2 = self.ontology.stanzas[a2.referent]
        return self.term_similarity(term1, term2)


class Wang_Entity(NumericScore):
    def __init__(self, ontology, weight):
        NumericScore.__init__(self)
        self.wang_normalization = Wang_Normalization(ontology, weight)

    def score(self, doc1, doc2, a1, a2):
        sem1 = a1.normalizations
        sem2 = a2.normalizations
#         sem1 = tuple(doc1.iternormalizations(a1))
#         sem2 = tuple(doc2.iternormalizations(a2))
        pairing12 = self.wang_normalization.pairing(doc1, doc2, sem1, sem2)
        pairing21 = self.wang_normalization.pairing(doc2, doc1, sem2, sem1)
        return (sum(s for (_, s) in pairing12.itervalues()) + sum(s for (_, s) in pairing21.itervalues())) / (len(sem1) + len(sem2))



OBO = '''! This file contains the OntoBiotope ontology, BioNLP-ST 2013 version.
! Copyright (C) 2013 Institut National de la Recherche Agronomique (INRA)
!
! The contents of this file is distributed under the Creative Commons CC-BY-SA license v3.0. The full details of the rights granted by this license can be found at the following address:
!     http://creativecommons.org/licenses/by-sa/3.0/


format-version: 1.2
date:  15:01:2013 17:56

[Term]
id: MBTO:00001967
name: soybean
exact_synonym: "Glycine hispida" [TyDI:23279]
exact_synonym: "Glycine max" [TyDI:23280]
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00001338
name: adipocyte
is_a: MBTO:00001852 ! cell

[Term]
id: MBTO:00001598
name: small ruminant
is_a: MBTO:00000826 ! ruminant

[Term]
id: MBTO:00000035
name: joint
is_a: MBTO:00000280 ! musculoskeletal system part

[Term]
id: MBTO:00000964
name: soft tick
exact_synonym: "soft-bodied tick" [TyDI:23272]
is_a: MBTO:00001118 ! tick

[Term]
id: MBTO:00000217
name: hospital water
is_a: MBTO:00001432 ! hospital environment

[Term]
id: MBTO:00000660
name: bedside carafe
is_a: MBTO:00000531 ! hospital drinking water

[Term]
id: MBTO:00001420
name: trichome
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00000044
name: spider
is_a: MBTO:00000605 ! arachnid

[Term]
id: MBTO:00000636
name: therapy equipment
is_a: MBTO:00001144 ! medical equipment

[Term]
id: MBTO:00001158
name: colon
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000255
name: sheep
is_a: MBTO:00000892 ! herbivore
is_a: MBTO:00000158 ! farm animal

[Term]
id: MBTO:00000843
name: shrub
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000231
name: salmonides
is_a: MBTO:00000561 ! fish

[Term]
id: MBTO:00001976
name: sharpshooter
is_a: MBTO:00000014 ! leafhopper

[Term]
id: MBTO:00000313
name: marine marsh sediment
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00001053
name: low salinity mud flat sediment
is_a: MBTO:00001855 ! mud sediment

[Term]
id: MBTO:00000684
name: freshwater sediment
is_a: MBTO:00000928 ! aquatic sediment

[Term]
id: MBTO:00000701
name: forest pond sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00001448
name: saline sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00000438
name: creek sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00000433
name: rice vinegar
is_a: MBTO:00000729 ! vinegar

[Term]
id: MBTO:00001921
name: peptic ulcer
is_a: MBTO:00000144 ! ulcer

[Term]
id: MBTO:00000324
name: duodenal ulcer
is_a: MBTO:00001921 ! peptic ulcer

[Term]
id: MBTO:00001902
name: estuarine sediment
is_a: MBTO:00001659 ! coastal sediment

[Term]
id: MBTO:00000863
name: ditch sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00000579
name: black sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00001768
name: herbicide enriched soil
is_a: MBTO:00000299 ! pesticide enriched soil

[Term]
id: MBTO:00001118
name: tick
is_a: MBTO:00000605 ! arachnid

[Term]
id: MBTO:00000553
name: tomato
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000948
name: skin abscess
is_a: MBTO:00001078 ! abscess
is_a: MBTO:00001857 ! skin lesion

[Term]
id: MBTO:00000772
name: tree
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00001142
name: skin papule
is_a: MBTO:00001209 ! skin part

[Term]
id: MBTO:00001653
name: gingival crevice
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001875
name: mummy tissue
is_a: MBTO:00000049 ! dead tissue

[Term]
id: MBTO:00000299
name: pesticide enriched soil
is_a: MBTO:00000198 ! soil contaminated with agricultural activity

[Term]
id: MBTO:00000816
name: person with untreated disease
exact_synonym: "untreated patient" [TyDI:25392]
is_a: MBTO:00002067 ! ill person

[Term]
id: MBTO:00000951
name: insecticide enriched soil
is_a: MBTO:00000299 ! pesticide enriched soil

[Term]
id: MBTO:00000239
name: conjunctiva
is_a: MBTO:00001202 ! eye part

[Term]
id: MBTO:00001281
name: squirrel
is_a: MBTO:00000108 ! rodent

[Term]
id: MBTO:00001195
name: star coral
is_a: MBTO:00001124 ! coral

[Term]
id: MBTO:00001689
name: sugar cane
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00001399
name: termite
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00001164
name: terrestrial crustacean
is_a: MBTO:00001772 ! crustacean

[Term]
id: MBTO:00000555
name: arctic marine sediments
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00000767
name: tidal flat sediment
is_a: MBTO:00001659 ! coastal sediment

[Term]
id: MBTO:00000182
name: sulfidic coastal sediment
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00000136
name: marine sulfidic sediment
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00001696
name: retail chicken
is_a: MBTO:00000873 ! chicken

[Term]
id: MBTO:00001632
name: liquid food
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00001193
name: person with untreated peritonitis
is_a: MBTO:00000816 ! person with untreated disease

[Term]
id: MBTO:00000096
name: person with untreated drug- resistant TB
is_a: MBTO:00001449 ! person with untreated TB

[Term]
id: MBTO:00000286
name: kidney
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001223
name: soda lake sediment
is_a: MBTO:00001397 ! lake sediment

[Term]
id: MBTO:00000694
name: salted lake sediment
is_a: MBTO:00001397 ! lake sediment

[Term]
id: MBTO:00000332
name: surface sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00002017
name: stream sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00000026
name: experimental medium
exact_synonym: "in vitro" [TyDI:27501]
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001442
name: ground beef
is_a: MBTO:00000947 ! beef

[Term]
id: MBTO:00001581
name: researcher
is_a: MBTO:00000414 ! scientist

[Term]
id: MBTO:00001275
name: duck
is_a: MBTO:00000804 ! poultry

[Term]
id: MBTO:00000804
name: poultry
is_a: MBTO:00000158 ! farm animal
is_a: MBTO:00000550 ! bird

[Term]
id: MBTO:00001781
name: worm
is_a: MBTO:00002014 ! nematode

[Term]
id: MBTO:00000946
name: contaminated site
is_a: MBTO:00000763 ! polluted environment

[Term]
id: MBTO:00001656
name: wombat
is_a: MBTO:00000892 ! herbivore

[Term]
id: MBTO:00001678
name: weed
is_a: MBTO:00000841 ! grass plant

[Term]
id: MBTO:00001742
name: vertebrate
is_a: MBTO:00001660 ! animal

[Term]
id: MBTO:00001123
name: warm-blooded animal
is_a: MBTO:00001742 ! vertebrate

[Term]
id: MBTO:00000307
name: puparia
is_a: MBTO:00002040 ! tsetse fly

[Term]
id: MBTO:00000618
name: brackish pond
is_a: MBTO:00001621 ! pond

[Term]
id: MBTO:00002040
name: tsetse fly
related_synonym: "tsetse host" [TyDI:23461]
exact_synonym: "tsetse" [TyDI:23462]
is_a: MBTO:00001069 ! blood-feeding insect
is_a: MBTO:00000264 ! fly

[Term]
id: MBTO:00001701
name: slaughter plant
related_synonym: "slaughterhouse" [TyDI:23465]
is_a: MBTO:00000176 ! food processing factory

[Term]
id: MBTO:00001812
name: industrial waste water treatment plant
is_a: MBTO:00002020 ! wastewater treatment plant

[Term]
id: MBTO:00000156
name: dairy wastewater treatment plant
is_a: MBTO:00001169 ! agricultural wastewater treatment plant

[Term]
id: MBTO:00001698
name: chemical plant
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00001667
name: salt lake mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001841
name: marine black mud
is_a: MBTO:00001177 ! marine mud

[Term]
id: MBTO:00001028
name: marine anoxic mud
is_a: MBTO:00001177 ! marine mud

[Term]
id: MBTO:00000421
name: hot mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00000659
name: spore
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00001187
name: goose
is_a: MBTO:00000804 ! poultry

[Term]
id: MBTO:00000858
name: college
is_a: MBTO:00001447 ! research and study center

[Term]
id: MBTO:00001386
name: turkey
is_a: MBTO:00000804 ! poultry

[Term]
id: MBTO:00001492
name: university
is_a: MBTO:00001447 ! research and study center

[Term]
id: MBTO:00001447
name: research and study center
is_a: MBTO:00000987 ! constructed habitat

[Term]
id: MBTO:00001903
name: arthropod part
exact_synonym: "arthropod organ" [TyDI:27105]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000197
name: unpasteurized milk
exact_synonym: "raw milk" [TyDI:23500]
is_a: MBTO:00000757 ! milk
is_a: MBTO:00001313 ! raw food
xref: ENVO:02000051 ! unpasteurized milk product

[Term]
id: MBTO:00000389
name: contaminated animal feed
is_a: MBTO:00000036 ! animal feed

[Term]
id: MBTO:00000036
name: animal feed
is_a: MBTO:00001254 ! industrial food

[Term]
id: MBTO:00001074
name: appendix
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001744
name: calcareous ooze
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00002053
name: endometrium
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001285
name: cell part
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00001242
name: rectum
related_synonym: "rectal" [TyDI:23517]
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001291
name: stomach
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000073
name: subgingiva
related_synonym: "subgingival" [TyDI:23522]
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00001059
name: upper respiratory tract
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00000196
name: young animal
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00000765
name: young adult
is_a: MBTO:00000196 ! young animal
is_a: MBTO:00000818 ! adult animal

[Term]
id: MBTO:00001334
name: pharyngeal mucosa
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00001715
name: pharynx
is_a: MBTO:00000164 ! respiratory tract part
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001009
name: saltern crystallizer pond
is_a: MBTO:00000976 ! saltern

[Term]
id: MBTO:00000482
name: black anoxic marine sediment
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00001532
name: anoxic river sediment
is_a: MBTO:00000130 ! river sediment

[Term]
id: MBTO:00000897
name: sedimentation pond
is_a: MBTO:00001621 ! pond

[Term]
id: MBTO:00000808
name: shallow pond
is_a: MBTO:00001621 ! pond

[Term]
id: MBTO:00000835
name: sewage oxidation pond
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00001682
name: phototroph
is_a: MBTO:00000394 ! living organism wrt energy source

[Term]
id: MBTO:00000848
name: catfish pond
is_a: MBTO:00000359 ! recreational fishing fish pond

[Term]
id: MBTO:00001153
name: water pollution treatment plant
is_a: MBTO:00002020 ! wastewater treatment plant

[Term]
id: MBTO:00000394
name: living organism wrt energy source
is_a: MBTO:00000297 ! living organism

[Term]
id: MBTO:00000274
name: surface soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001978
name: apple cider
is_a: MBTO:00000434 ! cider

[Term]
id: MBTO:00000314
name: gastric mucosa
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00001167
name: osteolytic bone lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001472
name: Intestinal mucosal lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00002052
name: louse
exact_synonym: "lice" [TyDI:23574]
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00000097
name: eye
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00000523
name: CF sputum
is_a: MBTO:00000406 ! sputum

[Term]
id: MBTO:00000548
name: male
is_a: MBTO:00000929 ! animal with age or sex property

[Term]
id: MBTO:00001296
name: embryo
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00001692
name: polar flagellum
is_a: MBTO:00001546 ! bacteria part

[Term]
id: MBTO:00001108
name: flagellum
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000960
name: marine eukaryotic species
is_a: MBTO:00001645 ! aquatic eukaryotic species

[Term]
id: MBTO:00001870
name: juice
is_a: MBTO:00000326 ! drink

[Term]
id: MBTO:00000208
name: maple
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00000113
name: fermented juice
is_a: MBTO:00001870 ! juice
is_a: MBTO:00001050 ! fermented beverage

[Term]
id: MBTO:00000705
name: heart
is_a: MBTO:00000797 ! organ
is_a: MBTO:00000277 ! circulatory system part

[Term]
id: MBTO:00001384
name: rum
is_a: MBTO:00000061 ! alcoholic drink

[Term]
id: MBTO:00001516
name: microorganism
exact_synonym: "microbe" [TyDI:23602]
related_synonym: "microbial" [TyDI:23603]
is_a: MBTO:00000297 ! living organism

[Term]
id: MBTO:00000061
name: alcoholic drink
is_a: MBTO:00000326 ! drink

[Term]
id: MBTO:00000226
name: mite
is_a: MBTO:00000605 ! arachnid

[Term]
id: MBTO:00000297
name: living organism
exact_synonym: "in vivo" [TyDI:27502]
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001909
name: periplasm
is_a: MBTO:00001285 ! cell part

[Term]
id: MBTO:00000469
name: deep periodontal lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00000100
name: periodontal lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001915
name: ulcerative lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001213
name: necrotic lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00000874
name: neumonic lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001439
name: deteriorated canned food
is_a: MBTO:00000740 ! canned food

[Term]
id: MBTO:00001575
name: freshwater mud
exact_synonym: "fresh water mud" [TyDI:23629]
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001602
name: nasal epithelia
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00000667
name: nasal cavity
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00001808
name: murine
is_a: MBTO:00000108 ! rodent

[Term]
id: MBTO:00001094
name: mouse
exact_synonym: "mice" [TyDI:23586]
is_a: MBTO:00001808 ! murine

[Term]
id: MBTO:00001642
name: cresote treated wood
is_a: MBTO:00001530 ! treated wood

[Term]
id: MBTO:00001818
name: creosote wood preservative-contaminated soil
is_a: MBTO:00000812 ! creosote contaminated soil

[Term]
id: MBTO:00002014
name: nematode
is_a: MBTO:00001947 ! invertebrate species

[Term]
id: MBTO:00001377
name: nose
related_synonym: "nasal" [TyDI:23652]
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00000776
name: oak
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00000569
name: root nodule
is_a: MBTO:00000771 ! root part
is_a: MBTO:00002066 ! plant nodule

[Term]
id: MBTO:00000568
name: photosynthetic apparatus
is_a: MBTO:00001546 ! bacteria part
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001994
name: oleander
is_a: MBTO:00000843 ! shrub

[Term]
id: MBTO:00000467
name: offspring
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00001547
name: anoxic environment
is_a: MBTO:00001434 ! environment wrt oxygen level

[Term]
id: MBTO:00002001
name: sulfide-rich water
is_a: MBTO:00000078 ! sulfide-rich environment
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00000443
name: aromatic hydrocarbon
is_a: MBTO:00000549 ! hydrocarbon

[Term]
id: MBTO:00001120
name: olive
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00000763
name: polluted environment
related_synonym: "contaminated site" [TyDI:23442]
is_a: MBTO:00000114 ! habitat wrt chemico-physical property

[Term]
id: MBTO:00001558
name: combustible liquid
is_a: MBTO:00000025 ! industrial chemical

[Term]
id: MBTO:00001647
name: biofuel
is_a: MBTO:00000679 ! fuel

[Term]
id: MBTO:00000581
name: kerosene
is_a: MBTO:00001558 ! combustible liquid

[Term]
id: MBTO:00000786
name: agricultural product
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00001555
name: crop
is_a: MBTO:00000786 ! agricultural product

[Term]
id: MBTO:00000040
name: stomach mucosa
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00001560
name: nasopharyngeal mucosa
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00001407
name: beach mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00000762
name: vestibular mucosa
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00000340
name: black anoxic freshwater mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001784
name: coastal lagoon mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001431
name: deep sea mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001233
name: ditch mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001303
name: Ornithodoros turicatae
is_a: MBTO:00000964 ! soft tick

[Term]
id: MBTO:00000118
name: Ornithodoros moubata
is_a: MBTO:00000964 ! soft tick

[Term]
id: MBTO:00001896
name: orange
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001789
name: skin
related_synonym: "cutaneous" [TyDI:23714]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001379
name: hot spring biomat
is_a: MBTO:00000937 ! biomat

[Term]
id: MBTO:00000615
name: dental abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00001458
name: subcutaneous abscess-like lesion
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00000422
name: soft tissue abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00001306
name: perineal abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00001758
name: plant cutting
is_a: MBTO:00001423 ! plant residue

[Term]
id: MBTO:00001300
name: intestinal epithelium
is_a: MBTO:00001889 ! epithelium
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001034
name: exoskeleton
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000461
name: insect part
is_a: MBTO:00001903 ! arthropod part

[Term]
id: MBTO:00001972
name: supragingival plaque
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00001270
name: spinal cord
is_a: MBTO:00000794 ! central nervous system

[Term]
id: MBTO:00001605
name: fruit
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001254
name: industrial food
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00000368
name: human louse
exact_synonym: "Pediculus humanus" [TyDI:23745]
is_a: MBTO:00002052 ! louse

[Term]
id: MBTO:00000229
name: poultry deep litter
is_a: MBTO:00001485 ! poultry litter

[Term]
id: MBTO:00000002
name: filarial nematode
is_a: MBTO:00001767 ! parasitic nematode

[Term]
id: MBTO:00000860
name: microorganism part
related_synonym: "microbe part" [TyDI:23759]
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00000078
name: sulfide-rich environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00000081
name: chlorosome
is_a: MBTO:00001546 ! bacteria part

[Term]
id: MBTO:00001714
name: gas vesicle
is_a: MBTO:00000860 ! microorganism part

[Term]
id: MBTO:00000782
name: surgical device
is_a: MBTO:00000455 ! hospital equipment

[Term]
id: MBTO:00001546
name: bacteria part
is_a: MBTO:00000860 ! microorganism part

[Term]
id: MBTO:00000448
name: aquaculture pond
is_a: MBTO:00001352 ! aquaculture habitat

[Term]
id: MBTO:00001966
name: medical center
is_a: MBTO:00000987 ! constructed habitat
is_a: MBTO:00000573 ! medical environment

[Term]
id: MBTO:00001819
name: liver abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00000648
name: nasal passage abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00001969
name: periodontal abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00000945
name: vaginal abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00000153
name: human Bartholin abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00001922
name: progeny
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00001508
name: potato
is_a: MBTO:00000851 ! tuber

[Term]
id: MBTO:00001438
name: rat
is_a: MBTO:00001808 ! murine

[Term]
id: MBTO:00000106
name: rabbit
is_a: MBTO:00000108 ! rodent

[Term]
id: MBTO:00000399
name: highly alkaline saline soda lake
is_a: MBTO:00001276 ! saline lake

[Term]
id: MBTO:00000687
name: geothermal lake
is_a: MBTO:00001370 ! lake

[Term]
id: MBTO:00000404
name: solar lake
is_a: MBTO:00001370 ! lake

[Term]
id: MBTO:00000245
name: hypersaline lake
is_a: MBTO:00001276 ! saline lake

[Term]
id: MBTO:00001828
name: digestive tract
related_synonym: "gastrointestinal tract" [TyDI:23802]
exact_synonym: "GI tract" [TyDI:23803]
related_synonym: "intestinal region" [TyDI:23805]
related_synonym: "gastrointestinal" [TyDI:23806]
exact_synonym: "GIT" [TyDI:23807]
related_synonym: "alimentary canal" [TyDI:24621]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00000583
name: ruminant livestock
is_a: MBTO:00000158 ! farm animal
is_a: MBTO:00000826 ! ruminant

[Term]
id: MBTO:00000476
name: lung
related_synonym: "pulmonary" [TyDI:23812]
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00000464
name: cervix
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00000908
name: fermented shrimp paste
is_a: MBTO:00000571 ! fermented food
is_a: MBTO:00001595 ! seafood

[Term]
id: MBTO:00000797
name: organ
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00002037
name: soil part
is_a: MBTO:00001833 ! environmental matter

[Term]
id: MBTO:00000567
name: urethra
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00000528
name: natural environment habitat
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00000540
name: rat flea
is_a: MBTO:00000506 ! flea

[Term]
id: MBTO:00000961
name: soil with chemical property
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001821
name: rice
exact_synonym: "Oryza sativa" [TyDI:30371]
is_a: MBTO:00000310 ! cereal

[Term]
id: MBTO:00000458
name: soil with physical property
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000343
name: reptile
is_a: MBTO:00001742 ! vertebrate

[Term]
id: MBTO:00000690
name: gum tissue
exact_synonym: "gingiva" [TyDI:27109]
related_synonym: "gingival" [TyDI:27110]
is_a: MBTO:00000866 ! mouth part
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00000015
name: surface of cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00001104
name: bite
is_a: MBTO:00000271 ! skin wound

[Term]
id: MBTO:00001944
name: cooling tower
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00000235
name: non-specified habitat
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001761
name: niche
is_a: MBTO:00000235 ! non-specified habitat

[Term]
id: MBTO:00000737
name: microcosm
is_a: MBTO:00000235 ! non-specified habitat

[Term]
id: MBTO:00002004
name: additive
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00000639
name: deep-sea hot vent
is_a: MBTO:00000145 ! hydrothermal vent

[Term]
id: MBTO:00001849
name: deep-sea hydrothermal vent
exact_synonym: "deep-sea hydrothermal vent site" [TyDI:23854]
is_a: MBTO:00000145 ! hydrothermal vent

[Term]
id: MBTO:00001717
name: alkaline lake
is_a: MBTO:00001370 ! lake

[Term]
id: MBTO:00000423
name: skeleton
is_a: MBTO:00000280 ! musculoskeletal system part

[Term]
id: MBTO:00000877
name: Livarot cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00001846
name: Italian Toma
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00000781
name: poultry farm
is_a: MBTO:00000380 ! farm

[Term]
id: MBTO:00001352
name: aquaculture habitat
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001540
name: mariculture farm
is_a: MBTO:00000980 ! aquaculture farm

[Term]
id: MBTO:00001953
name: mussel farm
is_a: MBTO:00001540 ! mariculture farm

[Term]
id: MBTO:00001452
name: fish farm
is_a: MBTO:00001540 ! mariculture farm

[Term]
id: MBTO:00001734
name: aquarium
is_a: MBTO:00000899 ! aquaculture equipment

[Term]
id: MBTO:00000290
name: harbor
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00001194
name: intertidal zone
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00000491
name: limestone
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00000500
name: pyritic acid mine drainage
is_a: MBTO:00000598 ! acid mine drainage

[Term]
id: MBTO:00001414
name: calcereous rock
is_a: MBTO:00000557 ! rock

[Term]
id: MBTO:00000468
name: rock scraping
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00000896
name: desert rock
is_a: MBTO:00000557 ! rock

[Term]
id: MBTO:00000557
name: rock
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00000699
name: shoreline
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00001045
name: tropical soil
is_a: MBTO:00000458 ! soil with physical property

[Term]
id: MBTO:00001088
name: granitic rock
is_a: MBTO:00000557 ! rock

[Term]
id: MBTO:00000988
name: shore
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00000599
name: chicken manure
related_synonym: "chicken waste" [TyDI:23905]
is_a: MBTO:00000560 ! livestock manure

[Term]
id: MBTO:00001862
name: human appendix abscess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00001215
name: siliceous ooze
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00001408
name: interstitial fluid
exact_synonym: "intercellular fluid" [TyDI:23912]
exact_synonym: "tissue fluid" [TyDI:23913]
is_a: MBTO:00000921 ! body fluid
xref: ENVO:02000042 ! interstitial fluid

[Term]
id: MBTO:00001977
name: lymph
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001478
name: mucus
related_synonym: "mucous flow" [TyDI:23918]
related_synonym: "mucous" [TyDI:23919]
is_a: MBTO:00000416 ! secretion

[Term]
id: MBTO:00000356
name: red clay
is_a: MBTO:00001655 ! clay

[Term]
id: MBTO:00002064
name: nasal secretion
is_a: MBTO:00000416 ! secretion

[Term]
id: MBTO:00001822
name: pus
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001942
name: synovial fluid
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001783
name: saliva
is_a: MBTO:00000416 ! secretion

[Term]
id: MBTO:00000428
name: sebum
is_a: MBTO:00000416 ! secretion

[Term]
id: MBTO:00000062
name: chyle
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000254
name: cerebrospinal fluid
exact_synonym: "CSF" [TyDI:23936]
exact_synonym: "liquor cerebrospinalis" [TyDI:23937]
exact_synonym: "cerebro-spinal fluid" [TyDI:23938]
is_a: MBTO:00000921 ! body fluid
xref: ENVO:02000029 ! cerebrospinal fluid

[Term]
id: MBTO:00001850
name: vitreous humor
exact_synonym: "vitreous humour" [TyDI:23941]
exact_synonym: "vitreous body" [TyDI:23942]
is_a: MBTO:00000921 ! body fluid
xref: ENVO:02000032 ! vitreous humor

[Term]
id: MBTO:00001199
name: breast milk
is_a: MBTO:00000416 ! secretion

[Term]
id: MBTO:00002033
name: tears
exact_synonym: "tear" [TyDI:23947]
is_a: MBTO:00000416 ! secretion
xref: ENVO:02000034 ! tears

[Term]
id: MBTO:00000075
name: pleural fluid
related_synonym: "pleural effusion" [TyDI:23950]
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001718
name: sweat
is_a: MBTO:00000008 ! excreta

[Term]
id: MBTO:00001993
name: chyme
exact_synonym: "chymus" [TyDI:23955]
is_a: MBTO:00000921 ! body fluid
xref: ENVO:02000026 ! chyme

[Term]
id: MBTO:00001661
name: blood plasma
related_synonym: "plasma" [TyDI:23958]
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001496
name: ear wax
exact_synonym: "cerumen" [TyDI:23961]
is_a: MBTO:00000921 ! body fluid
xref: ENVO:02000028 ! ear wax

[Term]
id: MBTO:00000041
name: aqueous humour
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001597
name: bile
related_synonym: "gall" [TyDI:23966]
is_a: MBTO:00000921 ! body fluid
xref: ENVO:02000023 ! bile

[Term]
id: MBTO:00000008
name: excreta
exact_synonym: "excretion" [TyDI:23969]
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000815
name: amniotic fluid
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001569
name: blood
related_synonym: "bloodstream" [TyDI:23974]
related_synonym: "blood stream" [TyDI:23975]
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000921
name: body fluid
exact_synonym: "bodily fluid" [TyDI:23978]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000757
name: milk
is_a: MBTO:00001246 ! dairy product

[Term]
id: MBTO:00000275
name: wine
is_a: MBTO:00001050 ! fermented beverage
is_a: MBTO:00000604 ! fermented fruit

[Term]
id: MBTO:00001246
name: dairy product
is_a: MBTO:00001847 ! animal product

[Term]
id: MBTO:00001110
name: endosymbiont microorganism
is_a: MBTO:00001516 ! microorganism

[Term]
id: MBTO:00000725
name: parasitic microorganism
is_a: MBTO:00001516 ! microorganism

[Term]
id: MBTO:00001732
name: free-living microorganism
is_a: MBTO:00001516 ! microorganism

[Term]
id: MBTO:00000017
name: pupa
related_synonym: "pupal" [TyDI:23993]
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00001163
name: trypanosome
is_a: MBTO:00000285 ! protozoa

[Term]
id: MBTO:00001069
name: blood-feeding insect
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00001992
name: iron-rich environment
is_a: MBTO:00000114 ! habitat wrt chemico-physical property

[Term]
id: MBTO:00000334
name: anaerobic environment
exact_synonym: "anoxic environments" [TyDI:27509]
is_a: MBTO:00001434 ! environment wrt oxygen level

[Term]
id: MBTO:00000558
name: aerobic environment
is_a: MBTO:00001434 ! environment wrt oxygen level

[Term]
id: MBTO:00001788
name: microaerophilic environment
related_synonym: "hypoxic environment" [TyDI:27452]
is_a: MBTO:00001434 ! environment wrt oxygen level

[Term]
id: MBTO:00001042
name: pesticide
is_a: MBTO:00001436 ! agricultural input

[Term]
id: MBTO:00000979
name: tip of the plant root
is_a: MBTO:00000771 ! root part

[Term]
id: MBTO:00001835
name: root hair
is_a: MBTO:00000771 ! root part

[Term]
id: MBTO:00001010
name: fertilizer
is_a: MBTO:00001436 ! agricultural input

[Term]
id: MBTO:00001436
name: agricultural input
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00000851
name: tuber
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000310
name: cereal
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00001657
name: aquatic plant
is_a: MBTO:00002027 ! plant
is_a: MBTO:00001645 ! aquatic eukaryotic species

[Term]
id: MBTO:00000379
name: terrestrial plant
is_a: MBTO:00002027 ! plant

[Term]
id: MBTO:00000101
name: bulbous plant
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00001324
name: subgingival biofilm
is_a: MBTO:00001456 ! host associated biofilm

[Term]
id: MBTO:00000586
name: dental biofilm
is_a: MBTO:00001456 ! host associated biofilm

[Term]
id: MBTO:00001608
name: skin ulcer
is_a: MBTO:00000144 ! ulcer

[Term]
id: MBTO:00000524
name: stomach ulcer
is_a: MBTO:00001921 ! peptic ulcer

[Term]
id: MBTO:00000014
name: leafhopper
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00001919
name: bone
is_a: MBTO:00000280 ! musculoskeletal system part
is_a: MBTO:00000272 ! vertebrate part

[Term]
id: MBTO:00001906
name: elkhorn coral
is_a: MBTO:00001124 ! coral

[Term]
id: MBTO:00002000
name: coral reef water
is_a: MBTO:00001481 ! marine water
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00001980
name: scleractinian coral
is_a: MBTO:00001124 ! coral

[Term]
id: MBTO:00001947
name: invertebrate species
related_synonym: "invertebrate" [TyDI:24056]
is_a: MBTO:00001660 ! animal

[Term]
id: MBTO:00000154
name: terminal airway
related_synonym: "airway" [TyDI:24059]
is_a: MBTO:00000476 ! lung

[Term]
id: MBTO:00000655
name: laboratory rat
is_a: MBTO:00001438 ! rat
is_a: MBTO:00000098 ! laboratory animal

[Term]
id: MBTO:00001111
name: larvae
related_synonym: "larval" [TyDI:24064]
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00001360
name: kangaroo
is_a: MBTO:00000892 ! herbivore

[Term]
id: MBTO:00000098
name: laboratory animal
is_a: MBTO:00001660 ! animal

[Term]
id: MBTO:00000833
name: cell culture
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00001746
name: curtain
is_a: MBTO:00000496 ! household good

[Term]
id: MBTO:00001891
name: feeder cell
is_a: MBTO:00000833 ! cell culture

[Term]
id: MBTO:00000046
name: leaching column
is_a: MBTO:00001451 ! waste treatment equipment

[Term]
id: MBTO:00000184
name: aerosol
related_synonym: "aerosolized" [TyDI:24079]
is_a: MBTO:00000611 ! air

[Term]
id: MBTO:00000029
name: reef surface biofilm
is_a: MBTO:00000189 ! biofilm in natural environment

[Term]
id: MBTO:00001127
name: granite stone
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00000876
name: intra-uterine progeny
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00001541
name: house
is_a: MBTO:00000987 ! constructed habitat

[Term]
id: MBTO:00000213
name: lime soap
is_a: MBTO:00000371 ! soap

[Term]
id: MBTO:00001705
name: wet environment
is_a: MBTO:00000114 ! habitat wrt chemico-physical property

[Term]
id: MBTO:00000114
name: habitat wrt chemico-physical property
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00000490
name: goat
is_a: MBTO:00000550 ! bird

[Term]
id: MBTO:00001210
name: graft recipient
is_a: MBTO:00001402 ! human

[Term]
id: MBTO:00001868
name: grape
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00000228
name: grapevine
exact_synonym: "grapewine" [TyDI:24105]
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000841
name: grass plant
exact_synonym: "grass" [TyDI:24108]
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000892
name: herbivore
is_a: MBTO:00001514 ! mammalian

[Term]
id: MBTO:00000719
name: canker
is_a: MBTO:00000478 ! fungi

[Term]
id: MBTO:00001048
name: city
related_synonym: "cities" [TyDI:24140]
is_a: MBTO:00000987 ! constructed habitat

[Term]
id: MBTO:00000629
name: meroplankton
is_a: MBTO:00000330 ! plankton

[Term]
id: MBTO:00001216
name: holoplankton
is_a: MBTO:00000330 ! plankton

[Term]
id: MBTO:00000686
name: planktonic
is_a: MBTO:00000330 ! plankton

[Term]
id: MBTO:00000330
name: plankton
is_a: MBTO:00000960 ! marine eukaryotic species

[Term]
id: MBTO:00000638
name: digestive chamber
is_a: MBTO:00001828 ! digestive tract
is_a: MBTO:00000110 ! intestine

[Term]
id: MBTO:00001830
name: dental caries
related_synonym: "tooth decay" [TyDI:30379]
exact_synonym: "dental cavity" [TyDI:30380]
is_a: MBTO:00002063 ! caries

[Term]
id: MBTO:00002063
name: caries
related_synonym: "caries lesion" [TyDI:24155]
related_synonym: "<caries lesion>" [TyDI:24156]
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001221
name: cortical bone
is_a: MBTO:00000280 ! musculoskeletal system part
is_a: MBTO:00000272 ! vertebrate part

[Term]
id: MBTO:00001636
name: endothelium
related_synonym: "endothelial" [TyDI:24174]
exact_synonym: "vascular endothelium" [TyDI:30617]
is_a: MBTO:00001318 ! membrane
is_a: MBTO:00000277 ! circulatory system part

[Term]
id: MBTO:00000507
name: extracellular
is_a: MBTO:00001852 ! cell

[Term]
id: MBTO:00000155
name: farmer
is_a: MBTO:00001055 ! worker

[Term]
id: MBTO:00000969
name: thermal area
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00000718
name: geothermal area
is_a: MBTO:00000969 ! thermal area

[Term]
id: MBTO:00001097
name: eschar
is_a: MBTO:00001857 ! skin lesion

[Term]
id: MBTO:00001889
name: epithelium
related_synonym: "epithelial" [TyDI:24192]
is_a: MBTO:00001487 ! epithelial layer

[Term]
id: MBTO:00001779
name: periodontal pocket
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00001380
name: traveler
is_a: MBTO:00001522 ! adult human

[Term]
id: MBTO:00001933
name: soldier
is_a: MBTO:00001055 ! worker

[Term]
id: MBTO:00001885
name: mound
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001659
name: coastal sediment
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00001219
name: marine coast
exact_synonym: "coastal marine environment" [TyDI:24209]
is_a: MBTO:00000086 ! coast

[Term]
id: MBTO:00000163
name: bacteria associated habitat
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00000990
name: almond tree
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00001039
name: coal spoil heap
is_a: MBTO:00001702 ! coal mine waste

[Term]
id: MBTO:00001444
name: nasopharynx
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00001796
name: sulfide mound
is_a: MBTO:00000078 ! sulfide-rich environment
is_a: MBTO:00001885 ! mound

[Term]
id: MBTO:00001652
name: lagoon
is_a: MBTO:00000429 ! coastal water

[Term]
id: MBTO:00000327
name: fresh meat
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00000474
name: coal
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00000125
name: coal spoil
related_synonym: "coal residue" [TyDI:24238]
is_a: MBTO:00000474 ! coal

[Term]
id: MBTO:00002046
name: coal-cleaning residue
is_a: MBTO:00001262 ! mine waste

[Term]
id: MBTO:00000796
name: acid hot spring
exact_synonym: "acidic hot spring" [TyDI:24245]
is_a: MBTO:00000415 ! hotspring
xref: ENVO:00002120 ! acid hot spring

[Term]
id: MBTO:00001457
name: desert
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00001729
name: coelomic cavity
exact_synonym: "coelom" [TyDI:24250]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001639
name: probiotic
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00000748
name: coelom fluid
exact_synonym: "coelomic fluid" [TyDI:24255]
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000335
name: swamp
exact_synonym: "wooded swamp" [TyDI:24260]
exact_synonym: "swamp forest" [TyDI:24261]
is_a: MBTO:00000564 ! wetland
xref: ENVO:00000043 ! wetland

[Term]
id: MBTO:00001155
name: hot tap water
is_a: MBTO:00000069 ! tap water

[Term]
id: MBTO:00000903
name: heart valve
is_a: MBTO:00000705 ! heart

[Term]
id: MBTO:00000788
name: benzene-contaminated site
is_a: MBTO:00000056 ! site contaminated with organic compound

[Term]
id: MBTO:00000152
name: contaminated groundwater
is_a: MBTO:00000840 ! contaminated water

[Term]
id: MBTO:00001988
name: petroleum refinery
is_a: MBTO:00001409 ! refinery

[Term]
id: MBTO:00000976
name: saltern
is_a: MBTO:00001941 ! high salt concentration environment
is_a: MBTO:00001753 ! artificial water environment
is_a: MBTO:00001114 ! lentic water body

[Term]
id: MBTO:00000283
name: brine pool
is_a: MBTO:00000382 ! brackish water

[Term]
id: MBTO:00001708
name: hyper saline brine sediment
related_synonym: "highly saline brine" [TyDI:24290]
is_a: MBTO:00001815 ! saline brine sediment

[Term]
id: MBTO:00001815
name: saline brine sediment
is_a: MBTO:00001448 ! saline sediment

[Term]
id: MBTO:00001927
name: self-heated organic material
is_a: MBTO:00000366 ! dead matter

[Term]
id: MBTO:00001208
name: ectomycorrhizal fungus
related_synonym: "ectomycorrhizal" [TyDI:24297]
is_a: MBTO:00000478 ! fungi

[Term]
id: MBTO:00000738
name: bug
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00001333
name: cockroach
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00001424
name: olive tree
is_a: MBTO:00001465 ! fruit tree

[Term]
id: MBTO:00001892
name: beer
is_a: MBTO:00000061 ! alcoholic drink
is_a: MBTO:00001050 ! fermented beverage

[Term]
id: MBTO:00000445
name: salting
related_synonym: "brined food" [TyDI:24308]
is_a: MBTO:00001007 ! curing

[Term]
id: MBTO:00000233
name: backwater
is_a: MBTO:00000824 ! lentic water

[Term]
id: MBTO:00000181
name: microflora
is_a: MBTO:00000163 ! bacteria associated habitat

[Term]
id: MBTO:00001720
name: teat
is_a: MBTO:00000693 ! mammalian part

[Term]
id: MBTO:00001044
name: honey bee
is_a: MBTO:00001571 ! bee

[Term]
id: MBTO:00000932
name: Lake Blankvann
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000669
name: hindgut
related_synonym: "hind-gut" [TyDI:24321]
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001571
name: bee
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00001526
name: poplar plantlet
is_a: MBTO:00000602 ! plantlet

[Term]
id: MBTO:00000351
name: turkey meat
is_a: MBTO:00001970 ! poultry meat

[Term]
id: MBTO:00001707
name: duck meat
is_a: MBTO:00001970 ! poultry meat

[Term]
id: MBTO:00001503
name: goose meat
is_a: MBTO:00001970 ! poultry meat

[Term]
id: MBTO:00001970
name: poultry meat
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00001671
name: chicken meat
related_synonym: "chicken" [TyDI:25844]
is_a: MBTO:00001970 ! poultry meat

[Term]
id: MBTO:00000991
name: gamefowl
is_a: MBTO:00000697 ! fowl

[Term]
id: MBTO:00001738
name: garlic
is_a: MBTO:00001651 ! Allium

[Term]
id: MBTO:00000071
name: Leguminosae
related_synonym: "pea family" [TyDI:24347]
related_synonym: "leguminous plant" [TyDI:24348]
exact_synonym: "Fabaceace" [TyDI:24349]
related_synonym: "legume" [TyDI:24350]
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000697
name: fowl
is_a: MBTO:00000550 ! bird

[Term]
id: MBTO:00001481
name: marine water
exact_synonym: "seawater" [TyDI:24908]
exact_synonym: "ocean water" [TyDI:24997]
exact_synonym: "sea water" [TyDI:24907]
is_a: MBTO:00000617 ! saline water
is_a: MBTO:00001257 ! marine environment
xref: ENVO:00002010 ! saline water

[Term]
id: MBTO:00000133
name: human body louse
is_a: MBTO:00000368 ! human louse

[Term]
id: MBTO:00001771
name: landfowl
is_a: MBTO:00000697 ! fowl

[Term]
id: MBTO:00001073
name: hen
is_a: MBTO:00000804 ! poultry

[Term]
id: MBTO:00000802
name: cheese
is_a: MBTO:00001735 ! fermented dairy product

[Term]
id: MBTO:00001290
name: ocean trench
is_a: MBTO:00001283 ! high pressure environment
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00000582
name: blueberry
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001801
name: plum
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001311
name: solar saltern
is_a: MBTO:00000976 ! saltern

[Term]
id: MBTO:00000007
name: strawberry
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001614
name: tomato
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001931
name: melon
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001184
name: avocado
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00000735
name: peach
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001963
name: gall bladder
is_a: MBTO:00000272 ! vertebrate part

[Term]
id: MBTO:00000807
name: apple
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00000378
name: pear
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001023
name: pregnant woman
is_a: MBTO:00000658 ! woman

[Term]
id: MBTO:00001911
name: drinking water facility
is_a: MBTO:00000535 ! artificial water structure

[Term]
id: MBTO:00001029
name: Alfalfa
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00000187
name: bone marrow
is_a: MBTO:00000272 ! vertebrate part
is_a: MBTO:00000510 ! lymphatic system part

[Term]
id: MBTO:00001062
name: onion
is_a: MBTO:00001651 ! Allium

[Term]
id: MBTO:00000771
name: root part
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001416
name: mung bean
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00000973
name: highly acid environment
is_a: MBTO:00001831 ! acid environment

[Term]
id: MBTO:00001568
name: gold mine wastewater
is_a: MBTO:00001998 ! mine waste water

[Term]
id: MBTO:00000138
name: sewage
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00000320
name: gold mine
is_a: MBTO:00000576 ! mine

[Term]
id: MBTO:00000576
name: mine
is_a: MBTO:00001473 ! extractive industrial site

[Term]
id: MBTO:00000598
name: acid mine drainage
exact_synonym: "AMD" [TyDI:24428]
is_a: MBTO:00001840 ! mine drainage
is_a: MBTO:00001831 ! acid environment

[Term]
id: MBTO:00001840
name: mine drainage
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00001700
name: mesenteric artery
is_a: MBTO:00001604 ! artery

[Term]
id: MBTO:00000369
name: abdomen
related_synonym: "abdominal" [TyDI:24435]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001528
name: contaminated soil with total petroleum hydrocarbon
exact_synonym: "contaminated soil with TPH" [TyDI:24438]
is_a: MBTO:00001422 ! oil contaminated soil

[Term]
id: MBTO:00001913
name: cotton cultivated soil
is_a: MBTO:00001502 ! agricultural soil
is_a: MBTO:00000810 ! cultivated habitat

[Term]
id: MBTO:00001107
name: cherry
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001895
name: cattle dipping
exact_synonym: "cattle dip" [TyDI:24445]
is_a: MBTO:00001939 ! liquid agricultural waste

[Term]
id: MBTO:00001754
name: raspberry
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001105
name: blackberry
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001017
name: volcanic area
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00001473
name: extractive industrial site
is_a: MBTO:00000247 ! industrial site

[Term]
id: MBTO:00001985
name: cattle-dipping vat
is_a: MBTO:00001170 ! agricultural equipment

[Term]
id: MBTO:00001188
name: leaf
exact_synonym: "plant leaf" [TyDI:24458]
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001520
name: plant surface
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001020
name: seed
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001081
name: fish pen
exact_synonym: "fish cage" [TyDI:24465]
is_a: MBTO:00000899 ! aquaculture equipment

[Term]
id: MBTO:00001501
name: mucocutaneous surface
is_a: MBTO:00001648 ! mucosal surface

[Term]
id: MBTO:00000326
name: drink
is_a: MBTO:00001632 ! liquid food

[Term]
id: MBTO:00000958
name: commensal
exact_synonym: "commensalism" [TyDI:24472]
is_a: MBTO:00001660 ! animal

[Term]
id: MBTO:00000004
name: sewage plant
exact_synonym: "sewage treatment plant" [TyDI:24475]
is_a: MBTO:00001888 ! waste treatment plant

[Term]
id: MBTO:00001131
name: cellulosic substrate
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00000145
name: hydrothermal vent
related_synonym: "hydrothermal vent system" [TyDI:24480]
is_a: MBTO:00001119 ! hydrotelluric environment

[Term]
id: MBTO:00001309
name: amazon
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000178
name: antenna
exact_synonym: "antennae" [TyDI:23557]
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001570
name: light harvesting antenna
exact_synonym: "light harvesting antennae" [TyDI:24485]
is_a: MBTO:00001546 ! bacteria part
is_a: MBTO:00000178 ! antenna

[Term]
id: MBTO:00002005
name: subtropical area
is_a: MBTO:00001482 ! area with climate property

[Term]
id: MBTO:00001874
name: perchlorate-contaminated site
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00001292
name: bacteriome
is_a: MBTO:00000461 ! insect part

[Term]
id: MBTO:00002012
name: red-pigmented bacteriome
is_a: MBTO:00001292 ! bacteriome

[Term]
id: MBTO:00001466
name: cervical
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001662
name: salmond egg
is_a: MBTO:00001596 ! fish product

[Term]
id: MBTO:00001440
name: home-prepared liver paste
is_a: MBTO:00000417 ! liver paste

[Term]
id: MBTO:00000981
name: chicken yard waste
is_a: MBTO:00000599 ! chicken manure

[Term]
id: MBTO:00001971
name: drainage canal
is_a: MBTO:00001825 ! water transport structure

[Term]
id: MBTO:00001863
name: drainage
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00000339
name: industrial water and effluent
is_a: MBTO:00000871 ! industrial habitat

[Term]
id: MBTO:00000811
name: kimchi
is_a: MBTO:00000893 ! fermented vegetable food

[Term]
id: MBTO:00000440
name: water of an humidifier
is_a: MBTO:00001679 ! water from air and water system

[Term]
id: MBTO:00001631
name: deep-sea hydrothermal vent chimney
is_a: MBTO:00000933 ! hydrothermal vent chimney

[Term]
id: MBTO:00000630
name: plant litter
is_a: MBTO:00001824 ! organic matter
is_a: MBTO:00001166 ! soil matter

[Term]
id: MBTO:00000164
name: respiratory tract part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000866
name: mouth part
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001998
name: mine waste water
exact_synonym: "mine wastewater" [TyDI:24524]
is_a: MBTO:00000179 ! waste water
is_a: MBTO:00001262 ! mine waste

[Term]
id: MBTO:00001688
name: dog
is_a: MBTO:00001512 ! domestic animal
is_a: MBTO:00001514 ! mammalian

[Term]
id: MBTO:00001512
name: domestic animal
is_a: MBTO:00001123 ! warm-blooded animal

[Term]
id: MBTO:00001883
name: wild animal
is_a: MBTO:00001742 ! vertebrate

[Term]
id: MBTO:00000742
name: ear part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000375
name: drinking water system
is_a: MBTO:00001911 ! drinking water facility
is_a: MBTO:00000331 ! water system

[Term]
id: MBTO:00000644
name: urogenital tract part
related_synonym: "genitourinary system part" [TyDI:30389]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001202
name: eye part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000956
name: phenol
is_a: MBTO:00000025 ! industrial chemical

[Term]
id: MBTO:00001391
name: creosol
is_a: MBTO:00000956 ! phenol

[Term]
id: MBTO:00001961
name: feed probiotics
is_a: MBTO:00000036 ! animal feed

[Term]
id: MBTO:00001567
name: rice-plant residue
is_a: MBTO:00001423 ! plant residue

[Term]
id: MBTO:00001423
name: plant residue
is_a: MBTO:00000050 ! plant material

[Term]
id: MBTO:00000432
name: marine farm fish
is_a: MBTO:00001315 ! farmed fish

[Term]
id: MBTO:00001315
name: farmed fish
exact_synonym: "farm fish" [TyDI:24563]
is_a: MBTO:00000561 ! fish

[Term]
id: MBTO:00001245
name: eel farm
is_a: MBTO:00001452 ! fish farm

[Term]
id: MBTO:00001453
name: tank water
is_a: MBTO:00001679 ! water from air and water system

[Term]
id: MBTO:00000898
name: oil reservoir
is_a: MBTO:00002008 ! extractive industry equipment

[Term]
id: MBTO:00000829
name: intestinal content
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000747
name: stomach content
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001349
name: posterior intestinal content
is_a: MBTO:00000829 ! intestinal content

[Term]
id: MBTO:00002054
name: creosote
is_a: MBTO:00000122 ! organic compound

[Term]
id: MBTO:00001989
name: polycyclic aromatic hydrocarbon
exact_synonym: "PAH" [TyDI:24580]
is_a: MBTO:00000443 ! aromatic hydrocarbon

[Term]
id: MBTO:00001908
name: sprout
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00001848
name: marsh
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00000564
name: wetland
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00001260
name: production water of an oil reservoir
is_a: MBTO:00000088 ! production water

[Term]
id: MBTO:00000059
name: peatland
related_synonym: "peat cutting area" [TyDI:24591]
related_synonym: "peatbog" [TyDI:24592]
related_synonym: "peat bog" [TyDI:24593]
is_a: MBTO:00000082 ! bog
xref: ENVO:00000043 ! wetland

[Term]
id: MBTO:00001113
name: production water from an oil well
is_a: MBTO:00000088 ! production water

[Term]
id: MBTO:00001200
name: freshwater marsh
is_a: MBTO:00001848 ! marsh
is_a: MBTO:00001057 ! freshwater wetland

[Term]
id: MBTO:00000248
name: oil well
is_a: MBTO:00001473 ! extractive industrial site

[Term]
id: MBTO:00000241
name: saline marsh
exact_synonym: "salt marsh" [TyDI:24602]
is_a: MBTO:00001848 ! marsh
is_a: MBTO:00000944 ! saline wetland
xref: ENVO:00000054 ! saline marsh

[Term]
id: MBTO:00001375
name: fuel ethanol production facility
is_a: MBTO:00001698 ! chemical plant

[Term]
id: MBTO:00001724
name: raised mire
related_synonym: "raised bog" [TyDI:24607]
is_a: MBTO:00000082 ! bog
xref: ENVO:00000185 ! raised mire

[Term]
id: MBTO:00001381
name: canal of root filled tooth
is_a: MBTO:00000298 ! dental root canal

[Term]
id: MBTO:00000595
name: peat cut
exact_synonym: "peat cutting" [TyDI:24612]
is_a: MBTO:00000050 ! plant material
xref: ENVO:00000157 ! peat cut

[Term]
id: MBTO:00000085
name: bacteriocyte
is_a: MBTO:00001338 ! adipocyte

[Term]
id: MBTO:00001183
name: tundra mire
related_synonym: "subarctic mire" [TyDI:24617]
is_a: MBTO:00000082 ! bog

[Term]
id: MBTO:00000088
name: production water
is_a: MBTO:00000179 ! waste water

[Term]
id: MBTO:00000107
name: alimentary canal
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000602
name: plantlet
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000717
name: dambo
is_a: MBTO:00001057 ! freshwater wetland

[Term]
id: MBTO:00001112
name: carr
is_a: MBTO:00000335 ! swamp

[Term]
id: MBTO:00000856
name: fen
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00000509
name: solfatara
related_synonym: "solfatara field" [TyDI:24632]
related_synonym: "fumarole field" [TyDI:24633]
is_a: MBTO:00001017 ! volcanic area

[Term]
id: MBTO:00001051
name: mudflat
is_a: MBTO:00000167 ! coastal wetland

[Term]
id: MBTO:00000954
name: peat swamp forest
exact_synonym: "peat swamp" [TyDI:24638]
is_a: MBTO:00000335 ! swamp

[Term]
id: MBTO:00001267
name: paisa mire
is_a: MBTO:00000082 ! bog

[Term]
id: MBTO:00000174
name: pocosin
is_a: MBTO:00001057 ! freshwater wetland

[Term]
id: MBTO:00001995
name: fluvial dambo
is_a: MBTO:00000717 ! dambo

[Term]
id: MBTO:00001103
name: vomit
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000315
name: tidal mudflat
related_synonym: "tidal flat" [TyDI:24675]
related_synonym: "intertidal mudflat" [TyDI:24676]
is_a: MBTO:00001051 ! mudflat
xref: ENVO:00000241 ! tidal mudflat

[Term]
id: MBTO:00000944
name: saline wetland
exact_synonym: "saline-wetland" [TyDI:24679]
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00001057
name: freshwater wetland
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00001810
name: spoiled milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00000609
name: cellulose
is_a: MBTO:00000025 ! industrial chemical

[Term]
id: MBTO:00001589
name: endolithic environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001397
name: lake sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00001984
name: blanket bog peat
is_a: MBTO:00000082 ! bog

[Term]
id: MBTO:00000341
name: feces
exact_synonym: "faeces" [TyDI:24733]
exact_synonym: "dung" [TyDI:24734]
related_synonym: "dropping" [TyDI:24735]
related_synonym: "pellet" [TyDI:24736]
related_synonym: "stool" [TyDI:24737]
related_synonym: "frass" [TyDI:24738]
related_synonym: "faecal" [TyDI:24739]
is_a: MBTO:00000008 ! excreta
xref: ENVO:00002003 ! feces

[Term]
id: MBTO:00000688
name: food
exact_synonym: "food product" [TyDI:24742]
related_synonym: "alimentary" [TyDI:24743]
related_synonym: "agro-alimentary environment" [TyDI:24744]
related_synonym: "foodborne" [TyDI:24745]
is_a: MBTO:00001254 ! industrial food

[Term]
id: MBTO:00000633
name: sediment
is_a: MBTO:00001166 ! soil matter

[Term]
id: MBTO:00000707
name: water
related_synonym: "aqueous" [TyDI:24749]
is_a: MBTO:00001833 ! environmental matter

[Term]
id: MBTO:00000617
name: saline water
exact_synonym: "salt water" [TyDI:24754]
is_a: MBTO:00000952 ! environmental water with chemical property
xref: ENVO:00002010 ! saline water

[Term]
id: MBTO:00000382
name: brackish water
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00000439
name: hypersaline water
is_a: MBTO:00000617 ! saline water

[Term]
id: MBTO:00000179
name: waste water
exact_synonym: "wastewater" [TyDI:24772]
is_a: MBTO:00000339 ! industrial water and effluent

[Term]
id: MBTO:00001773
name: soil
exact_synonym: "soilborne" [TyDI:27523]
is_a: MBTO:00001711 ! terrestrial habitat

[Term]
id: MBTO:00001588
name: natural gas-enriched soil
is_a: MBTO:00001089 ! organic compound contaminated soil

[Term]
id: MBTO:00001712
name: hopped wort
is_a: MBTO:00000345 ! beer wort

[Term]
id: MBTO:00001326
name: PAH contaminated soil
exact_synonym: "polycyclic aromatic hydrocarbon contaminated soil" [TyDI:24793]
is_a: MBTO:00000301 ! hydrocarbon contaminated soil

[Term]
id: MBTO:00001092
name: mercury-enriched soil
is_a: MBTO:00000727 ! heavy metal contaminated soil

[Term]
id: MBTO:00000204
name: dry sausage
is_a: MBTO:00000199 ! sausage

[Term]
id: MBTO:00000105
name: Balinese traditional fermented sausage
exact_synonym: "Urutan" [TyDI:24800]
is_a: MBTO:00000199 ! sausage

[Term]
id: MBTO:00000720
name: unhopped wort
is_a: MBTO:00000345 ! beer wort

[Term]
id: MBTO:00001367
name: home-made yogurt
is_a: MBTO:00000072 ! yogurt

[Term]
id: MBTO:00001691
name: fermented dry sausage
is_a: MBTO:00000204 ! dry sausage

[Term]
id: MBTO:00000920
name: ripened sausage
is_a: MBTO:00000199 ! sausage

[Term]
id: MBTO:00002031
name: wood
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00001867
name: ground water
exact_synonym: "groundwater" [TyDI:24813]
related_synonym: "subterranean water" [TyDI:26910]
exact_synonym: "underground water" [TyDI:26909]
is_a: MBTO:00001133 ! subterrestrial habitat
is_a: MBTO:00001141 ! environment water
xref: ENVO:00005792 ! underground water

[Term]
id: MBTO:00000593
name: eukaryote host
related_synonym: "host" [TyDI:24816]
related_synonym: "eukaryotic" [TyDI:24817]
exact_synonym: "eukaryotic host" [TyDI:24818]
related_synonym: "eukaryote" [TyDI:24819]
related_synonym: "bacteria host" [TyDI:24820]
is_a: MBTO:00000297 ! living organism

[Term]
id: MBTO:00001805
name: biofilm
related_synonym: "microbial slime" [TyDI:30390]
is_a: MBTO:00000163 ! bacteria associated habitat

[Term]
id: MBTO:00000662
name: sandstone
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00000854
name: oil spill
is_a: MBTO:00001262 ! mine waste

[Term]
id: MBTO:00001357
name: anaerobic sediment
exact_synonym: "anoxic sediment" [TyDI:24829]
is_a: MBTO:00000633 ! sediment
xref: ENVO:00002045 ! anaerobic sediment

[Term]
id: MBTO:00000559
name: urine
is_a: MBTO:00000008 ! excreta

[Term]
id: MBTO:00001227
name: oil seep
exact_synonym: "petroleum seep" [TyDI:24834]
is_a: MBTO:00001301 ! crude oil

[Term]
id: MBTO:00000890
name: fermented tomato juice
is_a: MBTO:00000113 ! fermented juice

[Term]
id: MBTO:00000170
name: fermented cabbage juice
is_a: MBTO:00000113 ! fermented juice

[Term]
id: MBTO:00001489
name: industrial effluent treatment plant
is_a: MBTO:00001888 ! waste treatment plant

[Term]
id: MBTO:00000695
name: drilling mud
is_a: MBTO:00001840 ! mine drainage

[Term]
id: MBTO:00000888
name: drilling pipe
is_a: MBTO:00002008 ! extractive industry equipment

[Term]
id: MBTO:00001797
name: irrigation ditch
is_a: MBTO:00001825 ! water transport structure

[Term]
id: MBTO:00001626
name: drainage ditch
is_a: MBTO:00001825 ! water transport structure

[Term]
id: MBTO:00000830
name: extreme environment
is_a: MBTO:00000114 ! habitat wrt chemico-physical property

[Term]
id: MBTO:00001831
name: acid environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00000032
name: alkaline environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001283
name: high pressure environment
exact_synonym: "high-pressure biotope" [TyDI:24861]
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001346
name: haline environment
related_synonym: "salty environment" [TyDI:24864]
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001832
name: high temperature environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001827
name: cold temperature environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001601
name: extreme high temperature environment
is_a: MBTO:00001832 ! high temperature environment

[Term]
id: MBTO:00002059
name: high osmolarity environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001491
name: Ice-cream factory
is_a: MBTO:00000176 ! food processing factory

[Term]
id: MBTO:00000024
name: chipboard factory
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00000511
name: nitrogen fertilizer factory
is_a: MBTO:00001698 ! chemical plant

[Term]
id: MBTO:00001542
name: vinegar factory
is_a: MBTO:00001907 ! brewery

[Term]
id: MBTO:00000402
name: sugar factory
is_a: MBTO:00000176 ! food processing factory

[Term]
id: MBTO:00001695
name: cheese factory
is_a: MBTO:00000481 ! food fermentation industry
is_a: MBTO:00001218 ! dairy industry

[Term]
id: MBTO:00001252
name: borax leachate
is_a: MBTO:00001476 ! leachate

[Term]
id: MBTO:00000412
name: chromate contaminated soil
is_a: MBTO:00000426 ! metal contaminated soil

[Term]
id: MBTO:00000759
name: arsenate treated wood
is_a: MBTO:00001530 ! treated wood

[Term]
id: MBTO:00001476
name: leachate
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00000005
name: anaerobic mud
exact_synonym: "anoxic mud" [TyDI:24895]
is_a: MBTO:00000281 ! mud
xref: ENVO:00002133 ! anaerobic mud

[Term]
id: MBTO:00002038
name: zinc factory
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00001530
name: treated wood
is_a: MBTO:00002031 ! wood

[Term]
id: MBTO:00000346
name: petrochemical factory
is_a: MBTO:00001698 ! chemical plant

[Term]
id: MBTO:00000045
name: chemical weapons factory
is_a: MBTO:00001698 ! chemical plant

[Term]
id: MBTO:00000813
name: Feta cheese brine
is_a: MBTO:00001007 ! curing

[Term]
id: MBTO:00000429
name: coastal water
exact_synonym: "coastal seawater" [TyDI:24912]
related_synonym: "coastal" [TyDI:24913]
is_a: MBTO:00001481 ! marine water
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00002023
name: xylene contaminated soil
is_a: MBTO:00000301 ! hydrocarbon contaminated soil

[Term]
id: MBTO:00001241
name: coal mine lake sediment
is_a: MBTO:00001397 ! lake sediment

[Term]
id: MBTO:00001736
name: packaging factory
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00000704
name: bottling factory
is_a: MBTO:00001736 ! packaging factory

[Term]
id: MBTO:00001918
name: latex processing factory
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00001368
name: canning factory
is_a: MBTO:00001736 ! packaging factory

[Term]
id: MBTO:00001462
name: contaminated sediment
is_a: MBTO:00000763 ! polluted environment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00001611
name: oil contaminated sediment
exact_synonym: "petroleum contaminated sediment" [TyDI:24936]
is_a: MBTO:00000074 ! sediment contaminated by organic pollutants

[Term]
id: MBTO:00000047
name: contaminated soil
related_synonym: "polluted soil" [TyDI:24939]
is_a: MBTO:00000946 ! contaminated site
is_a: MBTO:00000961 ! soil with chemical property

[Term]
id: MBTO:00000812
name: creosote contaminated soil
exact_synonym: "creosote-contaminated soil" [TyDI:24942]
is_a: MBTO:00000301 ! hydrocarbon contaminated soil

[Term]
id: MBTO:00000301
name: hydrocarbon contaminated soil
is_a: MBTO:00001089 ! organic compound contaminated soil

[Term]
id: MBTO:00001089
name: organic compound contaminated soil
exact_synonym: "carbon source enriched soil" [TyDI:24947]
is_a: MBTO:00001778 ! soil contaminated with industrial xenobiotic compound
is_a: MBTO:00000056 ! site contaminated with organic compound

[Term]
id: MBTO:00000642
name: marine sediment
exact_synonym: "pelagic sediment {alternative name}" [TyDI:24950]
is_a: MBTO:00000928 ! aquatic sediment
xref: ENVO:00002113 ! marine sediment

[Term]
id: MBTO:00000198
name: soil contaminated with agricultural activity
is_a: MBTO:00000047 ! contaminated soil

[Term]
id: MBTO:00001643
name: aerobic bioreactor
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00001152
name: sea sand
is_a: MBTO:00000703 ! sand

[Term]
id: MBTO:00000053
name: anaerobic bioreactor
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00000180
name: anaerobic dechlorinating bioreactor
is_a: MBTO:00000053 ! anaerobic bioreactor

[Term]
id: MBTO:00000915
name: soil of roadside tree
is_a: MBTO:00001271 ! roadside soil

[Term]
id: MBTO:00001539
name: urban soil
is_a: MBTO:00000047 ! contaminated soil

[Term]
id: MBTO:00000028
name: water dispenser
is_a: MBTO:00001161 ! drinking water supply

[Term]
id: MBTO:00000649
name: jungle
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00000939
name: arable soil
is_a: MBTO:00001502 ! agricultural soil

[Term]
id: MBTO:00001235
name: alfalfa silage
is_a: MBTO:00000710 ! silage

[Term]
id: MBTO:00001271
name: roadside soil
is_a: MBTO:00001539 ! urban soil

[Term]
id: MBTO:00000869
name: alpine soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000965
name: maize silage
is_a: MBTO:00000710 ! silage

[Term]
id: MBTO:00000253
name: corn silage
is_a: MBTO:00000338 ! grass silage

[Term]
id: MBTO:00000514
name: rice silage
is_a: MBTO:00000710 ! silage

[Term]
id: MBTO:00000793
name: grocery
is_a: MBTO:00001628 ! warehouse

[Term]
id: MBTO:00000689
name: orange storehouse
is_a: MBTO:00001628 ! warehouse

[Term]
id: MBTO:00000522
name: Kimchi
is_a: MBTO:00001272 ! pickled food

[Term]
id: MBTO:00000588
name: war readiness warehouse
is_a: MBTO:00001628 ! warehouse

[Term]
id: MBTO:00000895
name: meat
exact_synonym: "animal meat" [TyDI:24993]
is_a: MBTO:00001847 ! animal product

[Term]
id: MBTO:00000623
name: fermented Elaeis Palm sap
is_a: MBTO:00000113 ! fermented juice

[Term]
id: MBTO:00000287
name: malt vinegar brewery
is_a: MBTO:00001542 ! vinegar factory

[Term]
id: MBTO:00000894
name: offshore oil industry
is_a: MBTO:00001727 ! oil industry

[Term]
id: MBTO:00000222
name: high-level radioactive sediment
is_a: MBTO:00000462 ! radioactive sediment

[Term]
id: MBTO:00000481
name: food fermentation industry
is_a: MBTO:00000176 ! food processing factory

[Term]
id: MBTO:00000462
name: radioactive sediment
is_a: MBTO:00001462 ! contaminated sediment

[Term]
id: MBTO:00001341
name: electronics device industry
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00000111
name: ditch water
is_a: MBTO:00001753 ! artificial water environment

[Term]
id: MBTO:00000398
name: textile industry
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00000300
name: fermented sugar cane juice
is_a: MBTO:00000113 ! fermented juice

[Term]
id: MBTO:00001106
name: drilling bore water
is_a: MBTO:00000179 ! waste water

[Term]
id: MBTO:00001564
name: coastal aquifer
is_a: MBTO:00000723 ! aquifer

[Term]
id: MBTO:00000048
name: shallow coastal aquifer
is_a: MBTO:00001564 ! coastal aquifer

[Term]
id: MBTO:00000079
name: hay
is_a: MBTO:00001423 ! plant residue

[Term]
id: MBTO:00000447
name: upland
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00000013
name: decaying bark
is_a: MBTO:00000853 ! decaying wood

[Term]
id: MBTO:00000950
name: white fir tree
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00000077
name: alluvial gravel aquifer
is_a: MBTO:00000723 ! aquifer

[Term]
id: MBTO:00001358
name: bovine serum
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00000390
name: sand aquifer
exact_synonym: "sandy aquifer" [TyDI:25038]
is_a: MBTO:00000723 ! aquifer

[Term]
id: MBTO:00001168
name: gravel aquifer
is_a: MBTO:00000077 ! alluvial gravel aquifer

[Term]
id: MBTO:00001234
name: eucalyptus
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00002044
name: leafy soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000083
name: spruce
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00001523
name: water-stressed soil
is_a: MBTO:00000458 ! soil with physical property

[Term]
id: MBTO:00000986
name: greenhouse
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00000758
name: mountain
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00001982
name: ornithogenic soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000696
name: Cu-stressed soil
is_a: MBTO:00000426 ! metal contaminated soil

[Term]
id: MBTO:00000612
name: salt stressed soil
is_a: MBTO:00000058 ! salt contaminated soil

[Term]
id: MBTO:00000565
name: chemically stressed soil
is_a: MBTO:00000961 ! soil with chemical property

[Term]
id: MBTO:00000065
name: soil contaminated with used engine oil
is_a: MBTO:00001422 ! oil contaminated soil

[Term]
id: MBTO:00000006
name: cat
is_a: MBTO:00001512 ! domestic animal

[Term]
id: MBTO:00000370
name: cattle
is_a: MBTO:00000892 ! herbivore
is_a: MBTO:00000158 ! farm animal

[Term]
id: MBTO:00001509
name: calf
is_a: MBTO:00000472 ! bovine

[Term]
id: MBTO:00000574
name: carrier
related_synonym: "vector" [TyDI:25071]
is_a: MBTO:00001660 ! animal

[Term]
id: MBTO:00001699
name: coffee
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00001798
name: citrus fruit
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00000102
name: citrus
is_a: MBTO:00001465 ! fruit tree

[Term]
id: MBTO:00000726
name: cow
exact_synonym: "cows" [TyDI:25084]
is_a: MBTO:00000370 ! cattle
is_a: MBTO:00000472 ! bovine

[Term]
id: MBTO:00001124
name: coral
is_a: MBTO:00000960 ! marine eukaryotic species

[Term]
id: MBTO:00001056
name: volcanic soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001637
name: solfataric soil
is_a: MBTO:00001056 ! volcanic soil

[Term]
id: MBTO:00001093
name: permafrost
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000386
name: permafrost sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00001778
name: soil contaminated with industrial xenobiotic compound
is_a: MBTO:00000047 ! contaminated soil

[Term]
id: MBTO:00000857
name: flooded soil
is_a: MBTO:00000564 ! wetland
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001474
name: compost
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00001650
name: phenanthrene contaminated soil
exact_synonym: "phenanthrene-contaminated soil" [TyDI:25103]
is_a: MBTO:00001326 ! PAH contaminated soil

[Term]
id: MBTO:00001600
name: desert soil
related_synonym: "desert" [TyDI:24247]
is_a: MBTO:00001773 ! soil
xref: ENVO:00000097 ! desert

[Term]
id: MBTO:00000162
name: oil field
is_a: MBTO:00001473 ! extractive industrial site

[Term]
id: MBTO:00000543
name: crucifer
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000846
name: dairy cattle
is_a: MBTO:00000370 ! cattle

[Term]
id: MBTO:00001820
name: ornemental tree
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00001912
name: Drosophila melanogaster
is_a: MBTO:00001238 ! fruit fly

[Term]
id: MBTO:00001117
name: throat swab
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000465
name: rectal swab
is_a: MBTO:00000341 ! feces

[Term]
id: MBTO:00000158
name: farm animal
is_a: MBTO:00001512 ! domestic animal
is_a: MBTO:00000607 ! agricultural species

[Term]
id: MBTO:00000631
name: Euprymna scolopes
is_a: MBTO:00001900 ! bobtail squid

[Term]
id: MBTO:00001646
name: female cattle
is_a: MBTO:00000370 ! cattle

[Term]
id: MBTO:00001529
name: lamb
is_a: MBTO:00000255 ! sheep

[Term]
id: MBTO:00000322
name: porcine species
related_synonym: "porcine" [TyDI:25132]
is_a: MBTO:00000158 ! farm animal

[Term]
id: MBTO:00000459
name: rabbit tick
is_a: MBTO:00001118 ! tick

[Term]
id: MBTO:00001551
name: deep sea
exact_synonym: "deep-sea environment" [TyDI:25137]
exact_synonym: "deep-sea" [TyDI:25138]
is_a: MBTO:00001481 ! marine water
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00000924
name: deep subsurface
is_a: MBTO:00001133 ! subterrestrial habitat

[Term]
id: MBTO:00000321
name: surface water
is_a: MBTO:00001141 ! environment water

[Term]
id: MBTO:00000703
name: sand
is_a: MBTO:00001166 ! soil matter

[Term]
id: MBTO:00001331
name: rice paddy
related_synonym: "rice field" [TyDI:25147]
exact_synonym: "rice paddies" [TyDI:25148]
is_a: MBTO:00001803 ! cultivated field

[Term]
id: MBTO:00000801
name: dairy cow
exact_synonym: "dairy cows" [TyDI:25151]
is_a: MBTO:00000726 ! cow
is_a: MBTO:00000846 ! dairy cattle

[Term]
id: MBTO:00001016
name: marine sponge
is_a: MBTO:00000960 ! marine eukaryotic species
is_a: MBTO:00000168 ! sponge

[Term]
id: MBTO:00000168
name: sponge
is_a: MBTO:00001645 ! aquatic eukaryotic species

[Term]
id: MBTO:00000506
name: flea
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00000312
name: vaginal swab
is_a: MBTO:00001780 ! vaginal secretion

[Term]
id: MBTO:00001506
name: female animal
is_a: MBTO:00000929 ! animal with age or sex property

[Term]
id: MBTO:00001780
name: vaginal secretion
is_a: MBTO:00000416 ! secretion

[Term]
id: MBTO:00001816
name: female tsetse fly
is_a: MBTO:00002040 ! tsetse fly

[Term]
id: MBTO:00001534
name: Glossina
is_a: MBTO:00002040 ! tsetse fly

[Term]
id: MBTO:00000385
name: Glomus vesiculiferum
is_a: MBTO:00000478 ! fungi

[Term]
id: MBTO:00001465
name: fruit tree
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00001238
name: fruit fly
exact_synonym: "Drosophila" [TyDI:25115]
is_a: MBTO:00000264 ! fly

[Term]
id: MBTO:00000970
name: waterfowl
is_a: MBTO:00000697 ! fowl

[Term]
id: MBTO:00001860
name: broiler chicken
is_a: MBTO:00000873 ! chicken

[Term]
id: MBTO:00000473
name: furuncle fluid
is_a: MBTO:00001822 ! pus

[Term]
id: MBTO:00001256
name: semen
is_a: MBTO:00000416 ! secretion

[Term]
id: MBTO:00000917
name: transverse colon
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000891
name: furuncle
related_synonym: "boil" [TyDI:25188]
is_a: MBTO:00000948 ! skin abscess

[Term]
id: MBTO:00000621
name: throat
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00001014
name: perineum
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001616
name: mid-vaginal wall
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00000682
name: terminal ileum
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000099
name: sigmoid colon
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000092
name: gum margin
is_a: MBTO:00000690 ! gum tissue

[Term]
id: MBTO:00001638
name: arm
is_a: MBTO:00000454 ! primate part

[Term]
id: MBTO:00000240
name: left arm
is_a: MBTO:00001638 ! arm

[Term]
id: MBTO:00001013
name: right arm
is_a: MBTO:00001638 ! arm

[Term]
id: MBTO:00001721
name: foregut
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001247
name: gastric antrum
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001743
name: coal mine lake
is_a: MBTO:00000598 ! acid mine drainage

[Term]
id: MBTO:00000587
name: acid mine water
is_a: MBTO:00000598 ! acid mine drainage

[Term]
id: MBTO:00000487
name: arsenic-rich aquifer
is_a: MBTO:00001764 ! contaminated aquifer

[Term]
id: MBTO:00001582
name: sulfidogenic bioreactor
is_a: MBTO:00000053 ! anaerobic bioreactor

[Term]
id: MBTO:00001180
name: slough
is_a: MBTO:00001833 ! environmental matter

[Term]
id: MBTO:00000082
name: bog
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00000727
name: heavy metal contaminated soil
is_a: MBTO:00000426 ! metal contaminated soil

[Term]
id: MBTO:00000734
name: mire
is_a: MBTO:00001833 ! environmental matter

[Term]
id: MBTO:00000167
name: coastal wetland
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00001579
name: tidal marsh
is_a: MBTO:00001194 ! intertidal zone
is_a: MBTO:00000167 ! coastal wetland

[Term]
id: MBTO:00001932
name: natural gaz
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00000001
name: gaz seep
is_a: MBTO:00001932 ! natural gaz

[Term]
id: MBTO:00000516
name: methane seep
is_a: MBTO:00000001 ! gaz seep

[Term]
id: MBTO:00001955
name: orange juice
is_a: MBTO:00001870 ! juice

[Term]
id: MBTO:00001741
name: cranberry juice
is_a: MBTO:00001870 ! juice

[Term]
id: MBTO:00001032
name: grape juice
is_a: MBTO:00001870 ! juice

[Term]
id: MBTO:00000342
name: high temperature oil field
is_a: MBTO:00000162 ! oil field

[Term]
id: MBTO:00001419
name: arsenic contaminated-soil
is_a: MBTO:00001778 ! soil contaminated with industrial xenobiotic compound

[Term]
id: MBTO:00000984
name: meal
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00000060
name: drug
is_a: MBTO:00001171 ! medical product

[Term]
id: MBTO:00001897
name: paper manufacture
is_a: MBTO:00001938 ! manufacture

[Term]
id: MBTO:00001686
name: marine wetland
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00000716
name: meristem
related_synonym: "meristemic" [TyDI:25257]
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00000616
name: mesophyll
is_a: MBTO:00000568 ! photosynthetic apparatus

[Term]
id: MBTO:00001376
name: veterinary drug
is_a: MBTO:00000060 ! drug

[Term]
id: MBTO:00000842
name: mediterranean
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001469
name: industrial sludge
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00000306
name: invertebrate eating bird
exact_synonym: "bird that is feeding on invertebrate" [TyDI:25268]
is_a: MBTO:00000550 ! bird

[Term]
id: MBTO:00001061
name: coffee plant
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00000741
name: bladder stone
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00000893
name: fermented vegetable food
is_a: MBTO:00000571 ! fermented food
is_a: MBTO:00002021 ! vegetables

[Term]
id: MBTO:00000929
name: animal with age or sex property
is_a: MBTO:00001660 ! animal

[Term]
id: MBTO:00000251
name: oil pipeline
related_synonym: "petroleum pipeline" [TyDI:25279]
is_a: MBTO:00002008 ! extractive industry equipment

[Term]
id: MBTO:00000009
name: fermented Greek sausage
related_synonym: "Greek salami" [TyDI:25282]
is_a: MBTO:00000199 ! sausage

[Term]
id: MBTO:00000406
name: sputum
is_a: MBTO:00000008 ! excreta

[Term]
id: MBTO:00001923
name: contaminated milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00001080
name: Brassica
is_a: MBTO:00000543 ! crucifer

[Term]
id: MBTO:00000234
name: bovine milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00001339
name: seed eating bird
is_a: MBTO:00000550 ! bird

[Term]
id: MBTO:00000202
name: canned milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00001900
name: bobtail squid
is_a: MBTO:00000449 ! squid

[Term]
id: MBTO:00000022
name: butter milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00000374
name: bull
is_a: MBTO:00000472 ! bovine

[Term]
id: MBTO:00000237
name: brown dog tick
exact_synonym: "Rhipicephalus sanguineus" [TyDI:30402]
is_a: MBTO:00001036 ! dog tick

[Term]
id: MBTO:00002065
name: Brugia malayi
is_a: MBTO:00001562 ! human filarial nematode

[Term]
id: MBTO:00001002
name: cow's milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00001938
name: manufacture
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00000126
name: louse-born
is_a: MBTO:00002052 ! louse

[Term]
id: MBTO:00001083
name: petroleum-based fuel
is_a: MBTO:00000679 ! fuel

[Term]
id: MBTO:00001404
name: genital
is_a: MBTO:00001265 ! genital tract

[Term]
id: MBTO:00001886
name: gill
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00001006
name: guar gum
is_a: MBTO:00002004 ! additive

[Term]
id: MBTO:00002062
name: hair
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001428
name: human milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00001517
name: milk product
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00001964
name: meromictic lake
is_a: MBTO:00001370 ! lake

[Term]
id: MBTO:00001159
name: irradiated canned meat
is_a: MBTO:00000753 ! canned meat

[Term]
id: MBTO:00000989
name: clinic
exact_synonym: "clinical center" [TyDI:25333]
is_a: MBTO:00001966 ! medical center

[Term]
id: MBTO:00001314
name: raw hamburger meat
is_a: MBTO:00000325 ! hamburger meat

[Term]
id: MBTO:00001910
name: tomato-marinated broiler meat strip
is_a: MBTO:00001488 ! broiler meat strip

[Term]
id: MBTO:00000650
name: child
exact_synonym: "children" [TyDI:25342]
is_a: MBTO:00001402 ! human

[Term]
id: MBTO:00001676
name: French dry sausage
is_a: MBTO:00000204 ! dry sausage

[Term]
id: MBTO:00000708
name: amoebas
is_a: MBTO:00000285 ! protozoa

[Term]
id: MBTO:00001483
name: vacuum-packed meat
is_a: MBTO:00001782 ! packed meat

[Term]
id: MBTO:00000264
name: fly
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00000072
name: yogurt
is_a: MBTO:00001735 ! fermented dairy product

[Term]
id: MBTO:00000160
name: animal filarial nematode
is_a: MBTO:00000002 ! filarial nematode

[Term]
id: MBTO:00001406
name: Bulgarian yogurt
is_a: MBTO:00000072 ! yogurt

[Term]
id: MBTO:00000478
name: fungi
is_a: MBTO:00000593 ! eukaryote host

[Term]
id: MBTO:00000285
name: protozoa
is_a: MBTO:00000593 ! eukaryote host

[Term]
id: MBTO:00002050
name: Arabidopsis
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00001135
name: Arabidopsis thaliana
is_a: MBTO:00002050 ! Arabidopsis

[Term]
id: MBTO:00000679
name: fuel
is_a: MBTO:00000549 ! hydrocarbon

[Term]
id: MBTO:00001574
name: mother
is_a: MBTO:00000658 ! woman

[Term]
id: MBTO:00000305
name: patient with chronic liver disease
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001449
name: person with untreated TB
exact_synonym: "person with untreated pulmonary TB" [TyDI:25379]
exact_synonym: "person with untreated pulmonary tuberculosis" [TyDI:25380]
exact_synonym: "person with untreated tuberculosis" [TyDI:25381]
is_a: MBTO:00000816 ! person with untreated disease

[Term]
id: MBTO:00000387
name: patient with cystic fibrosis
exact_synonym: "CF patients" [TyDI:25384]
exact_synonym: "person with CF" [TyDI:25371]
exact_synonym: "person with cystic fibrosis" [TyDI:25372]
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001335
name: almond
is_a: MBTO:00001605 ! fruit

[Term]
id: MBTO:00000348
name: newborn infant
exact_synonym: "neonatal animal" [TyDI:30613]
exact_synonym: "neonate" [TyDI:26744]
exact_synonym: "newborn" [TyDI:23658]
is_a: MBTO:00000778 ! infant
is_a: MBTO:00000159 ! baby

[Term]
id: MBTO:00001460
name: Aeschynomene indica
is_a: MBTO:00000494 ! jointvetch

[Term]
id: MBTO:00001294
name: adult tsetse fly
is_a: MBTO:00002040 ! tsetse fly

[Term]
id: MBTO:00000778
name: infant
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00001762
name: man
is_a: MBTO:00000548 ! male
is_a: MBTO:00001522 ! adult human

[Term]
id: MBTO:00001201
name: immunodeficient person
exact_synonym: "compromised patient" [TyDI:24082]
exact_synonym: "immune-compromised person" [TyDI:30405]
exact_synonym: "compromised host" [TyDI:25405]
exact_synonym: "immune-compromised patient" [TyDI:24081]
exact_synonym: "immunodepressed person" [TyDI:30406]
exact_synonym: "immunodeppressed subject" [TyDI:25411]
exact_synonym: "immunocompromised host" [TyDI:25406]
is_a: MBTO:00002067 ! ill person

[Term]
id: MBTO:00000486
name: ABF pig
is_a: MBTO:00000882 ! pig

[Term]
id: MBTO:00001666
name: medical staff
related_synonym: "medical personnel" [TyDI:25413]
is_a: MBTO:00001055 ! worker

[Term]
id: MBTO:00000494
name: jointvetch
exact_synonym: "Aeschynomene" [TyDI:25416]
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00000768
name: extreme acid mine drainage
is_a: MBTO:00000598 ! acid mine drainage
is_a: MBTO:00001979 ! extremely acid environment

[Term]
id: MBTO:00000620
name: lake Magadi
exact_synonym: "Lake Magadi" [TyDI:25421]
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00002034
name: soda lake
is_a: MBTO:00000032 ! alkaline environment
is_a: MBTO:00001370 ! lake

[Term]
id: MBTO:00000057
name: Calcite Spring
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000033
name: maize
is_a: MBTO:00000310 ! cereal

[Term]
id: MBTO:00001561
name: pea
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00000110
name: intestine
related_synonym: "intestinal" [TyDI:27085]
exact_synonym: "intestinal tract" [TyDI:27084]
related_synonym: "enteric" [TyDI:27086]
related_synonym: "intestinal environment" [TyDI:27087]
related_synonym: "gut" [TyDI:23737]
related_synonym: "intestinal region" [TyDI:23805]
related_synonym: "enteroinvasive" [TyDI:27088]
is_a: MBTO:00000797 ! organ
is_a: MBTO:00000224 ! digestive tract part
xref: ENVO:00000029 ! watercourse

[Term]
id: MBTO:00001987
name: woody landscape
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00000943
name: catheter
is_a: MBTO:00001144 ! medical equipment

[Term]
id: MBTO:00001207
name: peripheral nervous system
is_a: MBTO:00001479 ! nervous system

[Term]
id: MBTO:00001402
name: human
related_synonym: "person" [TyDI:25453]
related_synonym: "individual" [TyDI:25454]
exact_synonym: "subject" [TyDI:25374]
exact_synonym: "homo sapiens" [TyDI:26681]
related_synonym: "people" [TyDI:25455]
is_a: MBTO:00001514 ! mammalian

[Term]
id: MBTO:00002067
name: ill person
is_a: MBTO:00001402 ! human

[Term]
id: MBTO:00000881
name: healthy person
related_synonym: "healthy human" [TyDI:25459]
related_synonym: "healthy individual" [TyDI:25460]
is_a: MBTO:00001402 ! human

[Term]
id: MBTO:00001974
name: blood vessel
is_a: MBTO:00000277 ! circulatory system part

[Term]
id: MBTO:00001672
name: decantation tank
is_a: MBTO:00001451 ! waste treatment equipment

[Term]
id: MBTO:00000535
name: artificial water structure
is_a: MBTO:00001753 ! artificial water environment

[Term]
id: MBTO:00000224
name: digestive tract part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001681
name: alcoholic person
is_a: MBTO:00002067 ! ill person

[Term]
id: MBTO:00000308
name: formula fed infant
is_a: MBTO:00000159 ! baby

[Term]
id: MBTO:00001658
name: community
is_a: MBTO:00001402 ! human

[Term]
id: MBTO:00001979
name: extremely acid environment
is_a: MBTO:00001831 ! acid environment

[Term]
id: MBTO:00000463
name: soda
is_a: MBTO:00000326 ! drink

[Term]
id: MBTO:00001623
name: surgery
related_synonym: "surgical" [TyDI:25515]
is_a: MBTO:00000455 ! hospital equipment

[Term]
id: MBTO:00000200
name: water tank
exact_synonym: "water storage tank" [TyDI:25518]
is_a: MBTO:00001911 ! drinking water facility

[Term]
id: MBTO:00001640
name: culture system
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000739
name: Lower Geyser Basin of Yellowstone National Park
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001538
name: ear
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001342
name: world
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000626
name: ear canal
is_a: MBTO:00000742 ! ear part

[Term]
id: MBTO:00001958
name: western Washington
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001829
name: outer ear
is_a: MBTO:00000742 ! ear part

[Term]
id: MBTO:00001709
name: buccal
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000865
name: worldwide
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000993
name: bachman road site
is_a: MBTO:00001350 ! western United States

[Term]
id: MBTO:00001355
name: saw mill
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00000371
name: soap
is_a: MBTO:00001510 ! household product

[Term]
id: MBTO:00000536
name: paper mill
exact_synonym: "paper factory" [TyDI:24927]
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00001336
name: southwestern United States
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001859
name: Southeast Asia
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001973
name: upstate NY
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000437
name: middle ear
is_a: MBTO:00000742 ! ear part

[Term]
id: MBTO:00001704
name: tropical Asian country
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001350
name: western United States
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001316
name: western hemisphere
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000269
name: sewage sludge
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00000656
name: sewage pipe
is_a: MBTO:00001451 ! waste treatment equipment

[Term]
id: MBTO:00001950
name: sewage disposal plant
is_a: MBTO:00000004 ! sewage plant

[Term]
id: MBTO:00000344
name: municipal sewage plant
exact_synonym: "communal sewage treatment plant" [TyDI:25583]
is_a: MBTO:00000004 ! sewage plant

[Term]
id: MBTO:00001437
name: domestic sewage
is_a: MBTO:00000138 ! sewage

[Term]
id: MBTO:00000665
name: orange tree
is_a: MBTO:00001465 ! fruit tree

[Term]
id: MBTO:00000563
name: scratch
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00002028
name: scald
is_a: MBTO:00001857 ! skin lesion

[Term]
id: MBTO:00001001
name: road
is_a: MBTO:00000613 ! road part

[Term]
id: MBTO:00000987
name: constructed habitat
is_a: MBTO:00000715 ! artificial environment

[Term]
id: MBTO:00000408
name: prepuce
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001143
name: quarry
is_a: MBTO:00001473 ! extractive industrial site

[Term]
id: MBTO:00001791
name: agricultural pest
is_a: MBTO:00000607 ! agricultural species

[Term]
id: MBTO:00000901
name: mammary gland
is_a: MBTO:00000693 ! mammalian part

[Term]
id: MBTO:00000852
name: home plumbing
exact_synonym: "home plumbing system" [TyDI:30414]
is_a: MBTO:00001295 ! domestic appliance

[Term]
id: MBTO:00001615
name: parenchyma
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00001934
name: ovary
related_synonym: "ovaries" [TyDI:25612]
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001620
name: peritrophic membrane
is_a: MBTO:00001903 ! arthropod part
is_a: MBTO:00000224 ! digestive tract part
is_a: MBTO:00001318 ! membrane

[Term]
id: MBTO:00001004
name: Siberia
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001467
name: tooth
exact_synonym: "dental" [TyDI:25623]
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00000021
name: Shiaskotan Island
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000784
name: northern Australia
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000051
name: north of France
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000935
name: North America
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000538
name: Mono Lake
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000112
name: Middle East
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00002003
name: Mariana Trench
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001400
name: Lake Khatyn
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001211
name: Munster cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00001239
name: cream cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00001226
name: Emmental cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00000753
name: canned meat
is_a: MBTO:00001782 ! packed meat
is_a: MBTO:00000740 ! canned food

[Term]
id: MBTO:00000325
name: hamburger meat
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00001533
name: urinary catheter
is_a: MBTO:00000943 ! catheter

[Term]
id: MBTO:00000751
name: plant-derived food
related_synonym: "food processing plant" [TyDI:25664]
related_synonym: "plant-derived foodstuff" [TyDI:25665]
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00000214
name: romadur cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00000279
name: Reblochon cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00001445
name: vegetable puree
is_a: MBTO:00002021 ! vegetables

[Term]
id: MBTO:00000923
name: smear-ripened cheese
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00000435
name: open-ocean
is_a: MBTO:00001481 ! marine water
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00001630
name: mudpit
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00000744
name: underground mine
is_a: MBTO:00000576 ! mine
is_a: MBTO:00001133 ! subterrestrial habitat

[Term]
id: MBTO:00002048
name: needle
is_a: MBTO:00001144 ! medical equipment

[Term]
id: MBTO:00001019
name: muscle
is_a: MBTO:00000280 ! musculoskeletal system part

[Term]
id: MBTO:00000109
name: periodontium
related_synonym: "periodontal" [TyDI:25693]
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00000243
name: foliar mesophyll
is_a: MBTO:00000616 ! mesophyll

[Term]
id: MBTO:00000502
name: open pit mine
exact_synonym: "opencast mine" [TyDI:25704]
is_a: MBTO:00000576 ! mine

[Term]
id: MBTO:00000497
name: indwelling urinary catheter
is_a: MBTO:00001533 ! urinary catheter

[Term]
id: MBTO:00000148
name: intravascular catheter
is_a: MBTO:00000943 ! catheter

[Term]
id: MBTO:00000585
name: wastewater treatment digester
is_a: MBTO:00001769 ! digester

[Term]
id: MBTO:00001769
name: digester
is_a: MBTO:00001451 ! waste treatment equipment

[Term]
id: MBTO:00001130
name: anaerobic digester
is_a: MBTO:00001769 ! digester

[Term]
id: MBTO:00000826
name: ruminant
is_a: MBTO:00001514 ! mammalian

[Term]
id: MBTO:00000790
name: shea cake digester
is_a: MBTO:00001769 ! digester

[Term]
id: MBTO:00001613
name: anaerobic wastewater digester
is_a: MBTO:00000585 ! wastewater treatment digester

[Term]
id: MBTO:00000505
name: thermophilic aerobic digester
is_a: MBTO:00001769 ! digester

[Term]
id: MBTO:00001873
name: urogenital tract
related_synonym: "urogenital" [TyDI:25738]
related_synonym: "urogenital area" [TyDI:25739]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001494
name: sour anaerobic digester
is_a: MBTO:00001130 ! anaerobic digester

[Term]
id: MBTO:00000124
name: respiratory tract
related_synonym: "respiratory" [TyDI:25744]
related_synonym: "respiratory tree" [TyDI:25745]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001140
name: municipal sewage sludge digester
is_a: MBTO:00001937 ! sludge digester

[Term]
id: MBTO:00000266
name: mosquito
is_a: MBTO:00001069 ! blood-feeding insect

[Term]
id: MBTO:00000591
name: thermophilic methanogenic bioreactor
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00001121
name: plant organ
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001502
name: agricultural soil
is_a: MBTO:00001773 ! soil
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00001433
name: sphagnum bog
related_synonym: "acidic Sphagnum peat bog" [TyDI:25764]
related_synonym: "Sphagnum peat bog" [TyDI:25765]
is_a: MBTO:00000082 ! bog

[Term]
id: MBTO:00000291
name: industrial waste
is_a: MBTO:00001790 ! waste

[Term]
id: MBTO:00001790
name: waste
is_a: MBTO:00000715 ! artificial environment

[Term]
id: MBTO:00001262
name: mine waste
is_a: MBTO:00000291 ! industrial waste

[Term]
id: MBTO:00001289
name: waste food compost
is_a: MBTO:00001474 ! compost

[Term]
id: MBTO:00001802
name: home food processing equipment
is_a: MBTO:00000496 ! household good

[Term]
id: MBTO:00001169
name: agricultural wastewater treatment plant
is_a: MBTO:00002020 ! wastewater treatment plant

[Term]
id: MBTO:00000561
name: fish
is_a: MBTO:00001742 ! vertebrate

[Term]
id: MBTO:00001793
name: industrial scrap
is_a: MBTO:00000291 ! industrial waste

[Term]
id: MBTO:00000361
name: salmond
is_a: MBTO:00000231 ! salmonides

[Term]
id: MBTO:00001587
name: dairy parlour waste
is_a: MBTO:00000779 ! dairy farming waste

[Term]
id: MBTO:00001125
name: slaughtering waste
is_a: MBTO:00001851 ! food processing waste

[Term]
id: MBTO:00000779
name: dairy farming waste
is_a: MBTO:00001027 ! agricultural waste

[Term]
id: MBTO:00000108
name: rodent
is_a: MBTO:00001514 ! mammalian

[Term]
id: MBTO:00001851
name: food processing waste
is_a: MBTO:00000183 ! industrial organic waste

[Term]
id: MBTO:00001756
name: human head louse
is_a: MBTO:00000368 ! human louse

[Term]
id: MBTO:00001531
name: salivary gland
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001651
name: Allium
is_a: MBTO:00000101 ! bulbous plant

[Term]
id: MBTO:00000183
name: industrial organic waste
is_a: MBTO:00000291 ! industrial waste

[Term]
id: MBTO:00000724
name: hemolymph
exact_synonym: "haemolymph" [TyDI:25804]
is_a: MBTO:00001903 ! arthropod part

[Term]
id: MBTO:00001192
name: phloem
is_a: MBTO:00001683 ! vascular tissue

[Term]
id: MBTO:00001197
name: animal waste
is_a: MBTO:00001997 ! organic waste

[Term]
id: MBTO:00001997
name: organic waste
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00001422
name: oil contaminated soil
exact_synonym: "oil-contaminated soil" [TyDI:25813]
related_synonym: "oil impregnated soil" [TyDI:25814]
is_a: MBTO:00000301 ! hydrocarbon contaminated soil

[Term]
id: MBTO:00001553
name: air conditioning system
exact_synonym: "air conditioner" [TyDI:30418]
is_a: MBTO:00000974 ! air treatment unit

[Term]
id: MBTO:00000296
name: oil
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00000927
name: solid agricultural waste
is_a: MBTO:00001027 ! agricultural waste

[Term]
id: MBTO:00000276
name: horticultural waste
is_a: MBTO:00001027 ! agricultural waste

[Term]
id: MBTO:00000560
name: livestock manure
is_a: MBTO:00001197 ! animal waste
is_a: MBTO:00001185 ! manure

[Term]
id: MBTO:00000872
name: bacteria habitat

[Term]
id: MBTO:00001185
name: manure
is_a: MBTO:00001939 ! liquid agricultural waste

[Term]
id: MBTO:00002026
name: household waste
is_a: MBTO:00001790 ! waste

[Term]
id: MBTO:00000733
name: building construction and demolition waste
is_a: MBTO:00001790 ! waste

[Term]
id: MBTO:00001027
name: agricultural waste
is_a: MBTO:00001790 ! waste

[Term]
id: MBTO:00000823
name: landfill site waste
is_a: MBTO:00002026 ! household waste

[Term]
id: MBTO:00000873
name: chicken
is_a: MBTO:00000804 ! poultry

[Term]
id: MBTO:00001939
name: liquid agricultural waste
is_a: MBTO:00001027 ! agricultural waste

[Term]
id: MBTO:00000550
name: bird
is_a: MBTO:00001123 ! warm-blooded animal

[Term]
id: MBTO:00001770
name: mining slag heap
is_a: MBTO:00001262 ! mine waste

[Term]
id: MBTO:00000472
name: bovine
is_a: MBTO:00000826 ! ruminant

[Term]
id: MBTO:00001033
name: swine
related_synonym: "hog" [TyDI:25855]
related_synonym: "pig" [TyDI:25856]
is_a: MBTO:00001514 ! mammalian

[Term]
id: MBTO:00000288
name: sugar-beet refinery
is_a: MBTO:00001409 ! refinery

[Term]
id: MBTO:00001425
name: factory
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00001409
name: refinery
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00001612
name: mushroom
is_a: MBTO:00000478 ! fungi

[Term]
id: MBTO:00001745
name: fenugreek
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00000238
name: sweet clover
related_synonym: "Melilotus" [TyDI:25877]
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00001825
name: water transport structure
is_a: MBTO:00000535 ! artificial water structure

[Term]
id: MBTO:00000350
name: rotting hay
is_a: MBTO:00001189 ! decaying matter

[Term]
id: MBTO:00001295
name: domestic appliance
is_a: MBTO:00000496 ! household good

[Term]
id: MBTO:00001880
name: cabbage
is_a: MBTO:00000571 ! fermented food

[Term]
id: MBTO:00000349
name: anaerobic sludge blanket reactor
is_a: MBTO:00000053 ! anaerobic bioreactor

[Term]
id: MBTO:00001905
name: thermophilic anaerobic methanogenic reactor
is_a: MBTO:00000053 ! anaerobic bioreactor

[Term]
id: MBTO:00001719
name: sulfide-oxidizing bioreactor
is_a: MBTO:00001643 ! aerobic bioreactor

[Term]
id: MBTO:00001565
name: cis-dichloroethene contaminated sediment
is_a: MBTO:00000074 ! sediment contaminated by organic pollutants

[Term]
id: MBTO:00000074
name: sediment contaminated by organic pollutants
exact_synonym: "organically contaminated sediment" [TyDI:25915]
is_a: MBTO:00001462 ! contaminated sediment

[Term]
id: MBTO:00001702
name: coal mine waste
related_synonym: "coal refuse" [TyDI:25918]
is_a: MBTO:00001262 ! mine waste
xref: ENVO:00002206 ! coal mine waste

[Term]
id: MBTO:00000434
name: cider
is_a: MBTO:00000061 ! alcoholic drink
is_a: MBTO:00001050 ! fermented beverage

[Term]
id: MBTO:00000381
name: inorganically contaminated sediment
is_a: MBTO:00001462 ! contaminated sediment

[Term]
id: MBTO:00000515
name: commercial yogurt
is_a: MBTO:00000072 ! yogurt

[Term]
id: MBTO:00000250
name: oligotrophic water
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00001750
name: eutrophic water
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00000754
name: mesotrophic water
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00000493
name: pond water
related_synonym: "pond" [TyDI:25951]
is_a: MBTO:00000824 ! lentic water
xref: ENVO:00000033 ! pond

[Term]
id: MBTO:00001240
name: pyrene
is_a: MBTO:00001989 ! polycyclic aromatic hydrocarbon

[Term]
id: MBTO:00001100
name: forest soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000673
name: lignocellulose
is_a: MBTO:00000609 ! cellulose

[Term]
id: MBTO:00000488
name: oil field water
related_synonym: "oil field production water" [TyDI:25988]
is_a: MBTO:00000179 ! waste water

[Term]
id: MBTO:00000403
name: nitrobenzene contaminated sediment
is_a: MBTO:00000074 ! sediment contaminated by organic pollutants

[Term]
id: MBTO:00000640
name: naphthalene contaminated sediment
is_a: MBTO:00000074 ! sediment contaminated by organic pollutants

[Term]
id: MBTO:00001394
name: pulp-bleaching waste water
is_a: MBTO:00000608 ! industrial wastewater

[Term]
id: MBTO:00000840
name: contaminated water
exact_synonym: "polluted water" [TyDI:25997]
is_a: MBTO:00000763 ! polluted environment
is_a: MBTO:00000952 ! environmental water with chemical property
xref: ENVO:00002186 ! contaminated water

[Term]
id: MBTO:00001498
name: hot dog
exact_synonym: "hot-dog" [TyDI:26000]
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00000517
name: prepared meat
exact_synonym: "processed meat" [TyDI:26003]
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00000215
name: intertidal sediment
is_a: MBTO:00001659 ! coastal sediment

[Term]
id: MBTO:00000417
name: liver paste
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00000884
name: salicylate enriched soil
is_a: MBTO:00002041 ! fertilized soil

[Term]
id: MBTO:00001690
name: acetamide enriched soil
is_a: MBTO:00001768 ! herbicide enriched soil
is_a: MBTO:00001089 ! organic compound contaminated soil

[Term]
id: MBTO:00000606
name: pantothenate enriched soil
is_a: MBTO:00002041 ! fertilized soil

[Term]
id: MBTO:00000426
name: metal contaminated soil
related_synonym: "metal-stressed soil" [TyDI:26036]
is_a: MBTO:00001778 ! soil contaminated with industrial xenobiotic compound

[Term]
id: MBTO:00001441
name: enriched soil
is_a: MBTO:00000961 ! soil with chemical property

[Term]
id: MBTO:00001454
name: ice cream
is_a: MBTO:00001246 ! dairy product

[Term]
id: MBTO:00000397
name: pork chop
is_a: MBTO:00000090 ! pork

[Term]
id: MBTO:00000090
name: pork
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00000947
name: beef
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00000338
name: grass silage
is_a: MBTO:00000710 ! silage

[Term]
id: MBTO:00000365
name: cured meat
is_a: MBTO:00000895 ! meat
is_a: MBTO:00001007 ! curing

[Term]
id: MBTO:00000868
name: drinking water
related_synonym: "water" [TyDI:23292]
related_synonym: "water table" [TyDI:26053]
is_a: MBTO:00000326 ! drink

[Term]
id: MBTO:00000345
name: beer wort
is_a: MBTO:00001763 ! wort

[Term]
id: MBTO:00000827
name: acidified beer wort
is_a: MBTO:00000345 ! beer wort

[Term]
id: MBTO:00001685
name: butter
is_a: MBTO:00001246 ! dairy product

[Term]
id: MBTO:00000592
name: pickled cabbage
is_a: MBTO:00001880 ! cabbage

[Term]
id: MBTO:00000710
name: silage
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00000902
name: animal manure
is_a: MBTO:00001197 ! animal waste

[Term]
id: MBTO:00001388
name: fresh animal manure
is_a: MBTO:00000902 ! animal manure

[Term]
id: MBTO:00001763
name: wort
is_a: MBTO:00000571 ! fermented food

[Term]
id: MBTO:00000393
name: trona crust
exact_synonym: "kaum crust" [TyDI:26072]
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00000836
name: mineral matter
is_a: MBTO:00001833 ! environmental matter

[Term]
id: MBTO:00001687
name: salt crust
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00000783
name: alkaline salt crust
is_a: MBTO:00001687 ! salt crust

[Term]
id: MBTO:00000466
name: alkaline soda lake
is_a: MBTO:00001717 ! alkaline lake
is_a: MBTO:00002034 ! soda lake

[Term]
id: MBTO:00001622
name: Lake Natron
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001128
name: Karkola
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000821
name: Guadeloupe
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000702
name: industrialized countries of the Northern Hemisphere
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001864
name: pasteurized milk
is_a: MBTO:00000757 ! milk

[Term]
id: MBTO:00002002
name: gastric acid
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001879
name: peritoneal fluid
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00001787
name: mineral soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00000150
name: vitreous fluid
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000372
name: dry forest humus
is_a: MBTO:00000267 ! forest humus

[Term]
id: MBTO:00001082
name: humus soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001137
name: pine forest humus
is_a: MBTO:00000267 ! forest humus

[Term]
id: MBTO:00001151
name: Cerrado
is_a: MBTO:00001844 ! savannah
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001586
name: Amazon rainforest
exact_synonym: "Amazon rain forest" [TyDI:26154]
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001278
name: teat canal
is_a: MBTO:00000693 ! mammalian part

[Term]
id: MBTO:00001522
name: adult human
is_a: MBTO:00001402 ! human
is_a: MBTO:00000818 ! adult animal

[Term]
id: MBTO:00001617
name: Florida Reef
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000575
name: Florida Keys
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001917
name: east Africa
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000205
name: Chesapeake Bay
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001559
name: sandy bulk soil
is_a: MBTO:00000614 ! bulk soil

[Term]
id: MBTO:00000547
name: humus
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00001706
name: canopy humus
is_a: MBTO:00000547 ! humus

[Term]
id: MBTO:00000267
name: forest humus
is_a: MBTO:00000547 ! humus

[Term]
id: MBTO:00001728
name: water-table aquifer
related_synonym: "drinking water aquifer" [TyDI:26186]
is_a: MBTO:00000723 ! aquifer

[Term]
id: MBTO:00000962
name: burnt soil
is_a: MBTO:00000458 ! soil with physical property

[Term]
id: MBTO:00000311
name: mud volcano
is_a: MBTO:00000714 ! volcano

[Term]
id: MBTO:00000614
name: bulk soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001459
name: aquifer contaminated with unleaded gasoline
is_a: MBTO:00001764 ! contaminated aquifer

[Term]
id: MBTO:00001764
name: contaminated aquifer
is_a: MBTO:00000152 ! contaminated groundwater
is_a: MBTO:00000723 ! aquifer

[Term]
id: MBTO:00000728
name: cold seep
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00001369
name: cold-seep sediment
is_a: MBTO:00001330 ! deep-sea sediment

[Term]
id: MBTO:00001954
name: Japan Trench
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001591
name: California
is_a: MBTO:00001755 ! USA states

[Term]
id: MBTO:00001493
name: United States
exact_synonym: "USA" [TyDI:26224]
exact_synonym: "U.S." [TyDI:26225]
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001203
name: Tanzania
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000042
name: Taiwan
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001172
name: Russia
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000971
name: Norway
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001030
name: Peru
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000265
name: Mauritius
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001566
name: New Zealand
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001024
name: Korea
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000460
name: Utah
is_a: MBTO:00001755 ! USA states

[Term]
id: MBTO:00000259
name: Africa
is_a: MBTO:00001175 ! continent

[Term]
id: MBTO:00000319
name: all continents
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001198
name: 24 US states
is_a: MBTO:00001755 ! USA states

[Term]
id: MBTO:00002015
name: Oregon
is_a: MBTO:00001755 ! USA states

[Term]
id: MBTO:00000068
name: Texas
is_a: MBTO:00001755 ! USA states

[Term]
id: MBTO:00001755
name: USA states
related_synonym: "US states" [TyDI:26256]
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000221
name: Europe
is_a: MBTO:00001175 ! continent

[Term]
id: MBTO:00000906
name: Florida
is_a: MBTO:00001755 ! USA states

[Term]
id: MBTO:00001899
name: New York
is_a: MBTO:00001755 ! USA states

[Term]
id: MBTO:00001480
name: Canada
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000188
name: Brazil
related_synonym: "Brazilian" [TyDI:26267]
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001098
name: Bengali
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001102
name: country
related_synonym: "nation" [TyDI:26272]
is_a: MBTO:00000963 ! geographical location
xref: ENVO:00000009 ! national geopolitical entity

[Term]
id: MBTO:00000244
name: England
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000556
name: Cuba
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000670
name: China
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001175
name: continent
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00000661
name: Asia
related_synonym: "Asian" [TyDI:26283]
is_a: MBTO:00001175 ! continent

[Term]
id: MBTO:00000519
name: Hungary
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00002013
name: island of Malta
exact_synonym: "Malta" [TyDI:26290]
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000839
name: Indonesia
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000539
name: Kenya
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000143
name: Japan
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001748
name: Ethiopia
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000525
name: Finland
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00000424
name: France
related_synonym: "French" [TyDI:26303]
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001040
name: Germany
is_a: MBTO:00001102 ! country

[Term]
id: MBTO:00001740
name: oil mill wastewater
is_a: MBTO:00000019 ! mill wastewater

[Term]
id: MBTO:00000652
name: municipal sludge
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00000019
name: mill wastewater
is_a: MBTO:00000608 ! industrial wastewater

[Term]
id: MBTO:00001759
name: chicken faeces
is_a: MBTO:00000599 ! chicken manure

[Term]
id: MBTO:00000295
name: saline wastewater
is_a: MBTO:00000179 ! waste water

[Term]
id: MBTO:00002045
name: municipal solid waste
is_a: MBTO:00002026 ! household waste

[Term]
id: MBTO:00001814
name: brewery wastewater
is_a: MBTO:00000608 ! industrial wastewater

[Term]
id: MBTO:00000875
name: malachite green effluent
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00001515
name: zinc- and sulfate-rich wastewater
is_a: MBTO:00001823 ! sulfate-rich wastewater

[Term]
id: MBTO:00000806
name: cattle waste
is_a: MBTO:00000560 ! livestock manure

[Term]
id: MBTO:00000039
name: shower curtain
is_a: MBTO:00001746 ! curtain

[Term]
id: MBTO:00001844
name: savannah
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00000050
name: plant material
is_a: MBTO:00000366 ! dead matter

[Term]
id: MBTO:00000130
name: river sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00000121
name: pentachlorophenol
is_a: MBTO:00001680 ! pesticide

[Term]
id: MBTO:00000436
name: petroleum reservoir
is_a: MBTO:00002008 ! extractive industry equipment

[Term]
id: MBTO:00001680
name: pesticide
is_a: MBTO:00000122 ! organic compound

[Term]
id: MBTO:00000116
name: denitrification reactor
exact_synonym: "denitrifying reactor" [TyDI:26374]
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00001087
name: landfill leachate
is_a: MBTO:00001549 ! organic leachate

[Term]
id: MBTO:00000207
name: rice-straw residue
is_a: MBTO:00001567 ! rice-plant residue

[Term]
id: MBTO:00000003
name: rice waste
is_a: MBTO:00001567 ! rice-plant residue

[Term]
id: MBTO:00000457
name: composting reactor
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00001881
name: cyanide treatment bioreactor
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00001937
name: sludge digester
is_a: MBTO:00001769 ! digester

[Term]
id: MBTO:00001392
name: sludge blanket reactor
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00001373
name: methanogenic reactor
is_a: MBTO:00000076 ! bioreactor

[Term]
id: MBTO:00001557
name: moor
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00001269
name: backswamp
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00001325
name: hypersaline microbial mat
is_a: MBTO:00000034 ! microbial mat

[Term]
id: MBTO:00001775
name: marine and hypersaline microbial mat
is_a: MBTO:00001929 ! marine microbial mat

[Term]
id: MBTO:00000746
name: barrier flat
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00000820
name: lower layer of a microbial mat
is_a: MBTO:00001134 ! microbial mat layer

[Term]
id: MBTO:00000034
name: microbial mat
is_a: MBTO:00000937 ! biomat

[Term]
id: MBTO:00001929
name: marine microbial mat
is_a: MBTO:00000034 ! microbial mat

[Term]
id: MBTO:00000966
name: oyster
is_a: MBTO:00001595 ! seafood

[Term]
id: MBTO:00000666
name: tropical zone
exact_synonym: "tropical area" [TyDI:24489]
is_a: MBTO:00001482 ! area with climate property

[Term]
id: MBTO:00000625
name: cheese starter culture
is_a: MBTO:00001353 ! dairy starter culture

[Term]
id: MBTO:00001684
name: temperate zone
is_a: MBTO:00001482 ! area with climate property

[Term]
id: MBTO:00001287
name: greenhouse soil
is_a: MBTO:00001502 ! agricultural soil

[Term]
id: MBTO:00000785
name: spinach
is_a: MBTO:00002021 ! vegetables

[Term]
id: MBTO:00001545
name: cheese spoilage
is_a: MBTO:00000802 ! cheese

[Term]
id: MBTO:00001606
name: terrestial wetland
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00000937
name: biomat
is_a: MBTO:00001805 ! biofilm

[Term]
id: MBTO:00000302
name: marine wetland
is_a: MBTO:00000564 ! wetland

[Term]
id: MBTO:00001134
name: microbial mat layer
is_a: MBTO:00000163 ! bacteria associated habitat

[Term]
id: MBTO:00000683
name: peat
is_a: MBTO:00001843 ! decaying plant material

[Term]
id: MBTO:00001669
name: clothe
is_a: MBTO:00000496 ! household good

[Term]
id: MBTO:00000766
name: sandy beach
is_a: MBTO:00001572 ! sandy soil

[Term]
id: MBTO:00000049
name: dead tissue
is_a: MBTO:00000366 ! dead matter

[Term]
id: MBTO:00000186
name: vegetable garden soil
is_a: MBTO:00001952 ! garden soil

[Term]
id: MBTO:00001041
name: tropical country
is_a: MBTO:00000666 ! tropical zone

[Term]
id: MBTO:00001133
name: subterrestrial habitat
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00001711
name: terrestrial habitat
related_synonym: "terrestrial" [TyDI:26473]
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00001952
name: garden soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00002020
name: wastewater treatment plant
exact_synonym: "waste water treatment plant" [TyDI:26478]
is_a: MBTO:00001888 ! waste treatment plant
is_a: MBTO:00000566 ! water treatment plant

[Term]
id: MBTO:00000882
name: pig
is_a: MBTO:00000322 ! porcine species
is_a: MBTO:00001033 ! swine

[Term]
id: MBTO:00001837
name: mummy
is_a: MBTO:00001139 ! dead animal

[Term]
id: MBTO:00001366
name: sandy sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00001572
name: sandy soil
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001468
name: beach sand
is_a: MBTO:00001618 ! coastal sand

[Term]
id: MBTO:00000011
name: coarse beach sand
is_a: MBTO:00001468 ! beach sand

[Term]
id: MBTO:00000069
name: tap water
is_a: MBTO:00000868 ! drinking water

[Term]
id: MBTO:00001122
name: shale sandstone
is_a: MBTO:00000662 ! sandstone

[Term]
id: MBTO:00000925
name: quinate enriched soil
is_a: MBTO:00000301 ! hydrocarbon contaminated soil

[Term]
id: MBTO:00002007
name: soap scum
is_a: MBTO:00000213 ! lime soap

[Term]
id: MBTO:00000216
name: Intertidal sand
is_a: MBTO:00001618 ! coastal sand

[Term]
id: MBTO:00001250
name: L-( null )-tartrate enriched soil
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00001618
name: coastal sand
is_a: MBTO:00000703 ! sand

[Term]
id: MBTO:00001482
name: area with climate property
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00000608
name: industrial wastewater
is_a: MBTO:00000179 ! waste water

[Term]
id: MBTO:00000347
name: food processing wastewater
is_a: MBTO:00000179 ! waste water

[Term]
id: MBTO:00000246
name: pig manure
is_a: MBTO:00000560 ! livestock manure

[Term]
id: MBTO:00001826
name: litter
is_a: MBTO:00001842 ! livestock habitat

[Term]
id: MBTO:00000407
name: industrial building
is_a: MBTO:00000247 ! industrial site

[Term]
id: MBTO:00001795
name: sulfide-rich freshwater sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00001549
name: organic leachate
is_a: MBTO:00001476 ! leachate

[Term]
id: MBTO:00001461
name: tannery
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00001071
name: spring sediment
is_a: MBTO:00000684 ! freshwater sediment

[Term]
id: MBTO:00001328
name: horse manure
related_synonym: "horse waste" [TyDI:26544]
is_a: MBTO:00000560 ! livestock manure

[Term]
id: MBTO:00001485
name: poultry litter
is_a: MBTO:00001826 ! litter

[Term]
id: MBTO:00001855
name: mud sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00001218
name: dairy industry
related_synonym: "milk industry" [TyDI:26555]
related_synonym: "dairy" [TyDI:26556]
is_a: MBTO:00000176 ! food processing factory
xref: ENVO:00000077 ! agricultural feature

[Term]
id: MBTO:00000601
name: sulfide-saturated mud sediment
is_a: MBTO:00000078 ! sulfide-rich environment
is_a: MBTO:00001855 ! mud sediment

[Term]
id: MBTO:00000176
name: food processing factory
exact_synonym: "food processing plant" [TyDI:25664]
exact_synonym: "food factory" [TyDI:26561]
is_a: MBTO:00001425 ! factory

[Term]
id: MBTO:00000963
name: geographical location
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001782
name: packed meat
is_a: MBTO:00001115 ! packed food

[Term]
id: MBTO:00001596
name: fish product
is_a: MBTO:00001847 ! animal product

[Term]
id: MBTO:00000740
name: canned food
is_a: MBTO:00001115 ! packed food

[Term]
id: MBTO:00000247
name: industrial site
is_a: MBTO:00000871 ! industrial habitat

[Term]
id: MBTO:00000206
name: road side
is_a: MBTO:00000613 ! road part

[Term]
id: MBTO:00000974
name: air treatment unit
is_a: MBTO:00001295 ! domestic appliance

[Term]
id: MBTO:00000933
name: hydrothermal vent chimney
is_a: MBTO:00001119 ! hydrotelluric environment

[Term]
id: MBTO:00002006
name: dune soil
related_synonym: "dune" [TyDI:26580]
is_a: MBTO:00001773 ! soil
xref: ENVO:00000170 ! dune

[Term]
id: MBTO:00000571
name: fermented food
is_a: MBTO:00001000 ! preserved food

[Term]
id: MBTO:00000070
name: compost biofilter
is_a: MBTO:00000054 ! biofilter

[Term]
id: MBTO:00000054
name: biofilter
is_a: MBTO:00001451 ! waste treatment equipment

[Term]
id: MBTO:00000975
name: pine litter
is_a: MBTO:00000630 ! plant litter

[Term]
id: MBTO:00001739
name: leaf litter
is_a: MBTO:00000630 ! plant litter

[Term]
id: MBTO:00001050
name: fermented beverage
is_a: MBTO:00000326 ! drink
is_a: MBTO:00000571 ! fermented food

[Term]
id: MBTO:00001421
name: fermented mare's milk
exact_synonym: "koumiss" [TyDI:26595]
is_a: MBTO:00001735 ! fermented dairy product

[Term]
id: MBTO:00000496
name: household good
is_a: MBTO:00000715 ! artificial environment

[Term]
id: MBTO:00001925
name: road junction
is_a: MBTO:00000613 ! road part

[Term]
id: MBTO:00001170
name: agricultural equipment
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00001510
name: household product
is_a: MBTO:00000496 ! household good

[Term]
id: MBTO:00001451
name: waste treatment equipment
is_a: MBTO:00000578 ! industrial equipment

[Term]
id: MBTO:00001888
name: waste treatment plant
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00002008
name: extractive industry equipment
is_a: MBTO:00001473 ! extractive industrial site

[Term]
id: MBTO:00000817
name: beer Shava
is_a: MBTO:00001892 ! beer

[Term]
id: MBTO:00001186
name: milking machine
is_a: MBTO:00001584 ! dairy farm equipment

[Term]
id: MBTO:00000712
name: bulk tank
exact_synonym: "bulk milk cooling tank" [TyDI:26616]
exact_synonym: "milk cooler" [TyDI:26617]
is_a: MBTO:00001584 ! dairy farm equipment

[Term]
id: MBTO:00000899
name: aquaculture equipment
is_a: MBTO:00001352 ! aquaculture habitat

[Term]
id: MBTO:00000530
name: dental plaque
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00000832
name: marine cage
is_a: MBTO:00000899 ! aquaculture equipment

[Term]
id: MBTO:00000225
name: anoxic sewage sludge
is_a: MBTO:00001641 ! digester sludge

[Term]
id: MBTO:00001149
name: oil sludge
is_a: MBTO:00001021 ! sludge

[Term]
id: MBTO:00000658
name: woman
is_a: MBTO:00001506 ! female animal
is_a: MBTO:00001522 ! adult human

[Term]
id: MBTO:00001583
name: anaerobic sewage sludge
is_a: MBTO:00001641 ! digester sludge

[Term]
id: MBTO:00000177
name: fermented beet
is_a: MBTO:00000571 ! fermented food

[Term]
id: MBTO:00000922
name: Bristish beer
is_a: MBTO:00001892 ! beer

[Term]
id: MBTO:00001824
name: organic matter
is_a: MBTO:00001833 ! environmental matter

[Term]
id: MBTO:00001852
name: cell
exact_synonym: "cellular" [TyDI:27520]
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00001641
name: digester sludge
is_a: MBTO:00000269 ! sewage sludge

[Term]
id: MBTO:00001610
name: wheat field
is_a: MBTO:00001803 ! cultivated field

[Term]
id: MBTO:00000292
name: deep tissue
is_a: MBTO:00001580 ! animal tissue

[Term]
id: MBTO:00001803
name: cultivated field
is_a: MBTO:00000261 ! field
is_a: MBTO:00000810 ! cultivated habitat

[Term]
id: MBTO:00001580
name: animal tissue
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000809
name: pasture
is_a: MBTO:00000261 ! field

[Term]
id: MBTO:00001165
name: cytoplasm
is_a: MBTO:00001285 ! cell part

[Term]
id: MBTO:00001584
name: dairy farm equipment
is_a: MBTO:00001170 ! agricultural equipment

[Term]
id: MBTO:00001505
name: cut
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001160
name: brain
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00000416
name: secretion
is_a: MBTO:00000921 ! body fluid

[Term]
id: MBTO:00000794
name: central nervous system
is_a: MBTO:00001479 ! nervous system

[Term]
id: MBTO:00001573
name: slash pine forest
is_a: MBTO:00000257 ! pine forest

[Term]
id: MBTO:00000257
name: pine forest
is_a: MBTO:00000157 ! forest

[Term]
id: MBTO:00000157
name: forest
is_a: MBTO:00001987 ! woody landscape

[Term]
id: MBTO:00001055
name: worker
is_a: MBTO:00001522 ! adult human

[Term]
id: MBTO:00001410
name: fish waste
is_a: MBTO:00000560 ! livestock manure

[Term]
id: MBTO:00000654
name: girl
is_a: MBTO:00000650 ! child

[Term]
id: MBTO:00000117
name: anaerobic digester sludge
is_a: MBTO:00001641 ! digester sludge

[Term]
id: MBTO:00001871
name: Troll gas field
is_a: MBTO:00000963 ! geographical location

[Term]
id: MBTO:00001521
name: geothermal aquifer
is_a: MBTO:00000723 ! aquifer

[Term]
id: MBTO:00000723
name: aquifer
is_a: MBTO:00000849 ! groundwater body

[Term]
id: MBTO:00001302
name: chemocline
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00001138
name: cooling water
is_a: MBTO:00001679 ! water from air and water system

[Term]
id: MBTO:00000508
name: decaying marine algae
is_a: MBTO:00001189 ! decaying matter

[Term]
id: MBTO:00000367
name: dust
exact_synonym: "dust particle" [TyDI:26695]
is_a: MBTO:00001833 ! environmental matter

[Term]
id: MBTO:00001330
name: deep-sea sediment
is_a: MBTO:00000642 ! marine sediment

[Term]
id: MBTO:00001833
name: environmental matter
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00001280
name: barn
is_a: MBTO:00001842 ! livestock habitat

[Term]
id: MBTO:00001119
name: hydrotelluric environment
related_synonym: "hydro-telluric" [TyDI:26706]
related_synonym: "hydrotelluric" [TyDI:26707]
is_a: MBTO:00000643 ! aquatic environment

[Term]
id: MBTO:00001261
name: chicken coop
is_a: MBTO:00001842 ! livestock habitat

[Term]
id: MBTO:00000983
name: harvesting tool
is_a: MBTO:00001578 ! agricultural tool

[Term]
id: MBTO:00001842
name: livestock habitat
is_a: MBTO:00000819 ! animal habitat

[Term]
id: MBTO:00001578
name: agricultural tool
is_a: MBTO:00001170 ! agricultural equipment

[Term]
id: MBTO:00001924
name: terrestrial landscape
is_a: MBTO:00001711 ! terrestrial habitat

[Term]
id: MBTO:00000714
name: volcano
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00001471
name: poultry house
is_a: MBTO:00000819 ! animal habitat

[Term]
id: MBTO:00001026
name: chicken house
is_a: MBTO:00001471 ! poultry house

[Term]
id: MBTO:00000173
name: pickles
is_a: MBTO:00001272 ! pickled food

[Term]
id: MBTO:00001272
name: pickled food
is_a: MBTO:00001007 ! curing

[Term]
id: MBTO:00001321
name: pickled herring
is_a: MBTO:00000936 ! pickled fish

[Term]
id: MBTO:00000936
name: pickled fish
related_synonym: "marinated fish" [TyDI:26734]
is_a: MBTO:00001272 ! pickled food

[Term]
id: MBTO:00001132
name: catfish
is_a: MBTO:00000561 ! fish

[Term]
id: MBTO:00000159
name: baby
is_a: MBTO:00001402 ! human

[Term]
id: MBTO:00000878
name: slaughter
is_a: MBTO:00001055 ! worker

[Term]
id: MBTO:00001674
name: welder
is_a: MBTO:00001055 ! worker

[Term]
id: MBTO:00001544
name: aquifer sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00000912
name: student
is_a: MBTO:00001522 ! adult human

[Term]
id: MBTO:00001996
name: granitic rock aquifer
is_a: MBTO:00000723 ! aquifer

[Term]
id: MBTO:00001145
name: chloroethene-contaminated aquifer
is_a: MBTO:00001764 ! contaminated aquifer

[Term]
id: MBTO:00001312
name: black smoker
is_a: MBTO:00000145 ! hydrothermal vent

[Term]
id: MBTO:00001488
name: broiler meat strip
is_a: MBTO:00001860 ! broiler chicken

[Term]
id: MBTO:00001115
name: packed food
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00001000
name: preserved food
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00001007
name: curing
related_synonym: "curing food" [TyDI:26763]
is_a: MBTO:00001000 ! preserved food

[Term]
id: MBTO:00000376
name: smoked food
is_a: MBTO:00001007 ! curing

[Term]
id: MBTO:00000880
name: smoked salmon
is_a: MBTO:00000376 ! smoked food
is_a: MBTO:00000384 ! fish flesh

[Term]
id: MBTO:00000542
name: district heating plant
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00000418
name: raw or improperly cooked shellfish
is_a: MBTO:00000377 ! raw shellfish

[Term]
id: MBTO:00000444
name: domestic wastewater treatment plant
is_a: MBTO:00002020 ! wastewater treatment plant

[Term]
id: MBTO:00000199
name: sausage
is_a: MBTO:00000895 ! meat

[Term]
id: MBTO:00001322
name: monument
is_a: MBTO:00000987 ! constructed habitat

[Term]
id: MBTO:00001664
name: raw ham
is_a: MBTO:00000090 ! pork
is_a: MBTO:00001965 ! undercooked meat

[Term]
id: MBTO:00000166
name: cellar
is_a: MBTO:00000987 ! constructed habitat

[Term]
id: MBTO:00000377
name: raw shellfish
is_a: MBTO:00001313 ! raw food
is_a: MBTO:00001595 ! seafood

[Term]
id: MBTO:00000789
name: drinking water reservoir
is_a: MBTO:00001911 ! drinking water facility

[Term]
id: MBTO:00000554
name: bottled water
is_a: MBTO:00000868 ! drinking water

[Term]
id: MBTO:00000731
name: ferret
is_a: MBTO:00001514 ! mammalian

[Term]
id: MBTO:00001313
name: raw food
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00000442
name: drinking water treatment plant
is_a: MBTO:00001911 ! drinking water facility
is_a: MBTO:00000566 ! water treatment plant

[Term]
id: MBTO:00001161
name: drinking water supply
is_a: MBTO:00001911 ! drinking water facility

[Term]
id: MBTO:00002056
name: fish pond
exact_synonym: "fish culture pond" [TyDI:26798]
is_a: MBTO:00001352 ! aquaculture habitat

[Term]
id: MBTO:00000580
name: field soil
is_a: MBTO:00001502 ! agricultural soil

[Term]
id: MBTO:00002041
name: fertilized soil
is_a: MBTO:00000198 ! soil contaminated with agricultural activity

[Term]
id: MBTO:00001511
name: urea enriched soil
is_a: MBTO:00002041 ! fertilized soil

[Term]
id: MBTO:00001229
name: sandstone monument
is_a: MBTO:00001322 ! monument

[Term]
id: MBTO:00000611
name: air
exact_synonym: "atmospheric" [TyDI:27515]
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00002021
name: vegetables
related_synonym: "legumes" [TyDI:26811]
is_a: MBTO:00000751 ! plant-derived food

[Term]
id: MBTO:00001965
name: undercooked meat
is_a: MBTO:00001313 ! raw food

[Term]
id: MBTO:00001595
name: seafood
is_a: MBTO:00001847 ! animal product

[Term]
id: MBTO:00000293
name: tidal creek
is_a: MBTO:00001194 ! intertidal zone

[Term]
id: MBTO:00001206
name: thermal spring
is_a: MBTO:00000822 ! spring

[Term]
id: MBTO:00001552
name: freshwater aquarium
is_a: MBTO:00001734 ! aquarium

[Term]
id: MBTO:00001752
name: warm coastal water
is_a: MBTO:00000429 ! coastal water
is_a: MBTO:00000955 ! environmental water with physical property

[Term]
id: MBTO:00000451
name: ash dump
exact_synonym: "ash dump site" [TyDI:26826]
exact_synonym: "ash dumping site" [TyDI:26827]
is_a: MBTO:00000291 ! industrial waste

[Term]
id: MBTO:00001129
name: warm seawater
is_a: MBTO:00001257 ! marine environment
is_a: MBTO:00001481 ! marine water
is_a: MBTO:00000955 ! environmental water with physical property

[Term]
id: MBTO:00001887
name: thermal power plant
is_a: MBTO:00000089 ! power plant

[Term]
id: MBTO:00001753
name: artificial water environment
is_a: MBTO:00000715 ! artificial environment

[Term]
id: MBTO:00000089
name: power plant
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00000477
name: flower
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00000590
name: toundra
is_a: MBTO:00001924 ! terrestrial landscape

[Term]
id: MBTO:00000485
name: pine
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00001173
name: sulfide-rich hot spring
related_synonym: "hotspring high in sulfide" [TyDI:26844]
is_a: MBTO:00001999 ! spring high in sulfide

[Term]
id: MBTO:00001847
name: animal product
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00000284
name: humus-rich acidic ash soil
is_a: MBTO:00000961 ! soil with chemical property
is_a: MBTO:00000547 ! humus

[Term]
id: MBTO:00000115
name: chloropicrine-enriched soil
is_a: MBTO:00001768 ! herbicide enriched soil

[Term]
id: MBTO:00001429
name: water in cooling tower
is_a: MBTO:00001138 ! cooling water

[Term]
id: MBTO:00000304
name: neuston
exact_synonym: "neuston biofilm" [TyDI:26857]
is_a: MBTO:00001805 ! biofilm

[Term]
id: MBTO:00000685
name: animal probiotic
is_a: MBTO:00000036 ! animal feed
is_a: MBTO:00001639 ! probiotic

[Term]
id: MBTO:00000736
name: shrimp culture pond
exact_synonym: "shrimp pond" [TyDI:26862]
is_a: MBTO:00000448 ! aquaculture pond

[Term]
id: MBTO:00000331
name: water system
related_synonym: "water supply" [TyDI:26865]
is_a: MBTO:00001825 ! water transport structure

[Term]
id: MBTO:00000492
name: abrasion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001078
name: abscess
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00000091
name: temperature sensor
is_a: MBTO:00001144 ! medical equipment

[Term]
id: MBTO:00001282
name: wound
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001857
name: skin lesion
related_synonym: "break in the skin" [TyDI:26876]
is_a: MBTO:00001590 ! lesion
is_a: MBTO:00001209 ! skin part

[Term]
id: MBTO:00000967
name: broncho-pulmonary segment
is_a: MBTO:00000476 ! lung

[Term]
id: MBTO:00000190
name: body
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000995
name: brain abcess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00000940
name: heat stressed soil
is_a: MBTO:00000458 ! soil with physical property

[Term]
id: MBTO:00000819
name: animal habitat
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00001025
name: bronchus
related_synonym: "branchial" [TyDI:26889]
is_a: MBTO:00000476 ! lung

[Term]
id: MBTO:00000058
name: salt contaminated soil
related_synonym: "salty soil" [TyDI:26892]
is_a: MBTO:00000047 ! contaminated soil
xref: ENVO:00005775 ! salt contaminated soil

[Term]
id: MBTO:00000573
name: medical environment
related_synonym: "nosocomial" [TyDI:26895]
related_synonym: "clinical" [TyDI:26896]
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00000455
name: hospital equipment
is_a: MBTO:00001144 ! medical equipment
is_a: MBTO:00001432 ! hospital environment

[Term]
id: MBTO:00000192
name: intensive care unit
is_a: MBTO:00001432 ! hospital environment

[Term]
id: MBTO:00000169
name: bedside water bottle
is_a: MBTO:00000531 ! hospital drinking water

[Term]
id: MBTO:00002039
name: hospital
related_synonym: "hospital environment" [TyDI:26905]
is_a: MBTO:00001966 ! medical center

[Term]
id: MBTO:00000825
name: humidifier
is_a: MBTO:00000974 ! air treatment unit

[Term]
id: MBTO:00001499
name: sterile water
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00001177
name: marine mud
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001716
name: muddy water
is_a: MBTO:00001141 ! environment water

[Term]
id: MBTO:00001363
name: respiratory therapy equipment
is_a: MBTO:00000636 ! therapy equipment

[Term]
id: MBTO:00001144
name: medical equipment
related_synonym: "medical care equipment" [TyDI:26921]
related_synonym: "medical supply" [TyDI:26922]
related_synonym: "medical care supply" [TyDI:26923]
is_a: MBTO:00000573 ! medical environment

[Term]
id: MBTO:00000914
name: sink
is_a: MBTO:00001144 ! medical equipment

[Term]
id: MBTO:00001694
name: bacon
is_a: MBTO:00000090 ! pork

[Term]
id: MBTO:00000384
name: fish flesh
is_a: MBTO:00001596 ! fish product

[Term]
id: MBTO:00000273
name: landfill contaminated by PCB
is_a: MBTO:00000905 ! PCB contaminated soil

[Term]
id: MBTO:00000905
name: PCB contaminated soil
related_synonym: "soil percolated with PCP" [TyDI:26933]
is_a: MBTO:00001089 ! organic compound contaminated soil

[Term]
id: MBTO:00000643
name: aquatic environment
related_synonym: "aquatic habitat" [TyDI:26938]
related_synonym: "aquatic" [TyDI:26939]
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00001191
name: anoxic zone of freshwater lake
is_a: MBTO:00000678 ! anoxic water

[Term]
id: MBTO:00000952
name: environmental water with chemical property
is_a: MBTO:00000114 ! habitat wrt chemico-physical property
is_a: MBTO:00001141 ! environment water

[Term]
id: MBTO:00001244
name: nitrogen-poor soil
is_a: MBTO:00000961 ! soil with chemical property

[Term]
id: MBTO:00001166
name: soil matter
is_a: MBTO:00002037 ! soil part

[Term]
id: MBTO:00001735
name: fermented dairy product
is_a: MBTO:00001246 ! dairy product
is_a: MBTO:00000571 ! fermented food

[Term]
id: MBTO:00000844
name: fermented fish
is_a: MBTO:00000571 ! fermented food
is_a: MBTO:00000384 ! fish flesh

[Term]
id: MBTO:00000134
name: straw
is_a: MBTO:00001423 ! plant residue

[Term]
id: MBTO:00001156
name: rice straw
is_a: MBTO:00000134 ! straw

[Term]
id: MBTO:00001926
name: industrial bakery
is_a: MBTO:00000176 ! food processing factory

[Term]
id: MBTO:00001513
name: rye grass silage
is_a: MBTO:00000338 ! grass silage

[Term]
id: MBTO:00000171
name: malt vinegar
exact_synonym: "Alegar" [TyDI:26964]
is_a: MBTO:00000729 ! vinegar

[Term]
id: MBTO:00000729
name: vinegar
is_a: MBTO:00001632 ! liquid food
is_a: MBTO:00000604 ! fermented fruit

[Term]
id: MBTO:00000400
name: cold soil
is_a: MBTO:00000458 ! soil with physical property

[Term]
id: MBTO:00002022
name: amended soil
is_a: MBTO:00001502 ! agricultural soil
is_a: MBTO:00000810 ! cultivated habitat

[Term]
id: MBTO:00000318
name: unamended soil
related_synonym: "unamended control soil" [TyDI:26977]
is_a: MBTO:00001502 ! agricultural soil
is_a: MBTO:00000810 ! cultivated habitat

[Term]
id: MBTO:00001077
name: buttermilk
is_a: MBTO:00001246 ! dairy product

[Term]
id: MBTO:00000604
name: fermented fruit
is_a: MBTO:00000571 ! fermented food

[Term]
id: MBTO:00001189
name: decaying matter
related_synonym: "saprophyte" [TyDI:26992]
related_synonym: "decaying organic matter" [TyDI:26993]
is_a: MBTO:00000366 ! dead matter

[Term]
id: MBTO:00001524
name: farmyard manure
is_a: MBTO:00000560 ! livestock manure

[Term]
id: MBTO:00001907
name: brewery
is_a: MBTO:00000481 ! food fermentation industry

[Term]
id: MBTO:00000938
name: tobacco warehouse
is_a: MBTO:00001628 ! warehouse

[Term]
id: MBTO:00001628
name: warehouse
related_synonym: "storehouse" [TyDI:27002]
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00000641
name: food processing effluent
is_a: MBTO:00001148 ! effluent

[Term]
id: MBTO:00001148
name: effluent
related_synonym: "industrial effluent" [TyDI:27007]
is_a: MBTO:00000339 ! industrial water and effluent

[Term]
id: MBTO:00000294
name: alkaline hotspring
exact_synonym: "alkaline hot spring" [TyDI:27010]
is_a: MBTO:00000415 ! hotspring

[Term]
id: MBTO:00001920
name: neutral hotspring
is_a: MBTO:00000415 ! hotspring

[Term]
id: MBTO:00000822
name: spring
is_a: MBTO:00002016 ! lotic water body

[Term]
id: MBTO:00001999
name: spring high in sulfide
exact_synonym: "sulfide-rich spring" [TyDI:27017]
is_a: MBTO:00002001 ! sulfide-rich water
is_a: MBTO:00000822 ! spring

[Term]
id: MBTO:00001576
name: part of living organism
related_synonym: "host part" [TyDI:27020]
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001370
name: lake
exact_synonym: "freshwater lake" [TyDI:26935]
is_a: MBTO:00001114 ! lentic water body

[Term]
id: MBTO:00000824
name: lentic water
related_synonym: "stagnant water" [TyDI:27024]
is_a: MBTO:00001141 ! environment water

[Term]
id: MBTO:00000603
name: stratified lake
is_a: MBTO:00001370 ! lake

[Term]
id: MBTO:00001943
name: sake
is_a: MBTO:00000326 ! drink

[Term]
id: MBTO:00000165
name: salt pork
is_a: MBTO:00000090 ! pork

[Term]
id: MBTO:00000270
name: sake brewery
is_a: MBTO:00001907 ! brewery

[Term]
id: MBTO:00001279
name: soy sauce
is_a: MBTO:00001951 ! fermented soybean
is_a: MBTO:00000855 ! sauce

[Term]
id: MBTO:00000441
name: stable manure
is_a: MBTO:00000902 ! animal manure

[Term]
id: MBTO:00001951
name: fermented soybean
is_a: MBTO:00000893 ! fermented vegetable food

[Term]
id: MBTO:00001095
name: dry soil
is_a: MBTO:00000458 ! soil with physical property

[Term]
id: MBTO:00001463
name: estuary
is_a: MBTO:00001194 ! intertidal zone

[Term]
id: MBTO:00000862
name: flowing water
related_synonym: "running water" [TyDI:30458]
is_a: MBTO:00001141 ! environment water
is_a: MBTO:00002016 ! lotic water body

[Term]
id: MBTO:00000955
name: environmental water with physical property
is_a: MBTO:00000114 ! habitat wrt chemico-physical property
is_a: MBTO:00001141 ! environment water

[Term]
id: MBTO:00000415
name: hotspring
exact_synonym: "hot spring" [TyDI:27049]
is_a: MBTO:00000822 ! spring
xref: ENVO:00000051 ! hot spring

[Term]
id: MBTO:00002036
name: freshwater hotspring
is_a: MBTO:00000415 ! hotspring

[Term]
id: MBTO:00001116
name: low nutrient aquatic habitat
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00001383
name: low temperature ground water
is_a: MBTO:00001867 ! ground water
is_a: MBTO:00000955 ! environmental water with physical property

[Term]
id: MBTO:00000526
name: chlorophenol-contaminated groundwater
is_a: MBTO:00000152 ! contaminated groundwater
is_a: MBTO:00001867 ! ground water

[Term]
id: MBTO:00000749
name: creamery
is_a: MBTO:00001218 ! dairy industry

[Term]
id: MBTO:00000316
name: air filter
is_a: MBTO:00000974 ! air treatment unit

[Term]
id: MBTO:00001273
name: kefir
is_a: MBTO:00001735 ! fermented dairy product

[Term]
id: MBTO:00000721
name: potato silage
is_a: MBTO:00000710 ! silage

[Term]
id: MBTO:00001890
name: sour milk
is_a: MBTO:00000757 ! milk
is_a: MBTO:00001735 ! fermented dairy product

[Term]
id: MBTO:00001265
name: genital tract
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00000504
name: urinary tract
related_synonym: "urinary" [TyDI:27076]
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00000298
name: dental root canal
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00000395
name: animal part
related_synonym: "animal host part" [TyDI:27081]
related_synonym: "body part" [TyDI:27082]
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00001776
name: extra-intestinal
exact_synonym: "extraintestinal" [TyDI:27091]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001487
name: epithelial layer
is_a: MBTO:00001580 ! animal tissue

[Term]
id: MBTO:00001495
name: fat body
is_a: MBTO:00000461 ! insect part

[Term]
id: MBTO:00001625
name: extra-genital
exact_synonym: "extragenital" [TyDI:27098]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001190
name: jejunum
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000392
name: ileum
related_synonym: "ileum of the small intestine" [TyDI:27103]
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000850
name: arthropod
is_a: MBTO:00001660 ! animal

[Term]
id: MBTO:00000752
name: mucosal tissue
related_synonym: "mucosa" [TyDI:27113]
is_a: MBTO:00001403 ! lining

[Term]
id: MBTO:00000791
name: gingival sulcus
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00000999
name: mouth
related_synonym: "oral" [TyDI:27118]
related_synonym: "oral cavity" [TyDI:27119]
is_a: MBTO:00001709 ! buccal

[Term]
id: MBTO:00001393
name: germ cell
is_a: MBTO:00001852 ! cell

[Term]
id: MBTO:00001418
name: germline
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001986
name: extra-uterus
related_synonym: "extra-uterine" [TyDI:27126]
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00000847
name: intestinal mucosa
exact_synonym: "gut mucosa" [TyDI:27476]
is_a: MBTO:00000224 ! digestive tract part
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00001806
name: intra-uterus
related_synonym: "intra-uterine" [TyDI:27134]
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001068
name: uterus
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001126
name: large intestine
is_a: MBTO:00000110 ! intestine

[Term]
id: MBTO:00001263
name: small intestine
is_a: MBTO:00000110 ! intestine

[Term]
id: MBTO:00001817
name: intercellular
related_synonym: "intercellularly" [TyDI:27143]
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00001946
name: intracellular
related_synonym: "intracellularly" [TyDI:27146]
is_a: MBTO:00001285 ! cell part

[Term]
id: MBTO:00001182
name: duodenum
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001504
name: rhizosphere
exact_synonym: "soil rhizosphere" [TyDI:23434]
related_synonym: "area surrounding plant root" [TyDI:27151]
related_synonym: "root zone" [TyDI:27152]
is_a: MBTO:00001773 ! soil
is_a: MBTO:00001121 ! plant organ

[Term]
id: MBTO:00001660
name: animal
related_synonym: "animal host" [TyDI:27155]
exact_synonym: "animal-associated habitat" [TyDI:27156]
related_synonym: "animal species" [TyDI:27157]
is_a: MBTO:00000593 ! eukaryote host

[Term]
id: MBTO:00002027
name: plant
related_synonym: "host plant" [TyDI:27160]
exact_synonym: "green plant" [TyDI:27161]
is_a: MBTO:00000593 ! eukaryote host

[Term]
id: MBTO:00001514
name: mammalian
exact_synonym: "mammalia-associated habitat" [TyDI:27164]
related_synonym: "mammalia" [TyDI:27165]
related_synonym: "mammal" [TyDI:27166]
related_synonym: "mammalian host" [TyDI:27167]
is_a: MBTO:00001123 ! warm-blooded animal

[Term]
id: MBTO:00000141
name: insect
related_synonym: "insect host" [TyDI:27170]
is_a: MBTO:00000850 ! arthropod

[Term]
id: MBTO:00000281
name: mud
related_synonym: "ooze" [TyDI:27173]
is_a: MBTO:00001166 ! soil matter

[Term]
id: MBTO:00000715
name: artificial environment
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001021
name: sludge
is_a: MBTO:00000281 ! mud

[Term]
id: MBTO:00001395
name: nest
is_a: MBTO:00000819 ! animal habitat

[Term]
id: MBTO:00001525
name: rodent nest
is_a: MBTO:00001395 ! nest

[Term]
id: MBTO:00000409
name: granuloma
is_a: MBTO:00000764 ! immune cell

[Term]
id: MBTO:00000380
name: farm
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00000764
name: immune cell
is_a: MBTO:00001852 ! cell

[Term]
id: MBTO:00000038
name: dairy farm
is_a: MBTO:00000380 ! farm

[Term]
id: MBTO:00001196
name: rainwater
is_a: MBTO:00000904 ! freshwater

[Term]
id: MBTO:00000532
name: agricultural habitat
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001308
name: xylem
is_a: MBTO:00001683 ! vascular tissue

[Term]
id: MBTO:00001683
name: vascular tissue
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001066
name: vagina
related_synonym: "vaginal" [TyDI:27200]
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001236
name: pig farm
is_a: MBTO:00000380 ! farm

[Term]
id: MBTO:00001205
name: tuberculoid granuloma
is_a: MBTO:00000409 ! granuloma

[Term]
id: MBTO:00001621
name: pond
related_synonym: "freshwater pond" [TyDI:23542]
is_a: MBTO:00001114 ! lentic water body

[Term]
id: MBTO:00000828
name: river
is_a: MBTO:00002016 ! lotic water body

[Term]
id: MBTO:00000086
name: coast
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00001361
name: brine
is_a: MBTO:00000283 ! brine pool

[Term]
id: MBTO:00000354
name: water canal
exact_synonym: "canal" [TyDI:27214]
is_a: MBTO:00002016 ! lotic water body
xref: ENVO:00000037 ! ditch

[Term]
id: MBTO:00001139
name: dead animal
is_a: MBTO:00001957 ! dead organism

[Term]
id: MBTO:00001785
name: activated sludge
is_a: MBTO:00001641 ! digester sludge

[Term]
id: MBTO:00001064
name: botanical garden soil
is_a: MBTO:00001952 ! garden soil

[Term]
id: MBTO:00001677
name: garden
is_a: MBTO:00000810 ! cultivated habitat

[Term]
id: MBTO:00001063
name: dead body
is_a: MBTO:00001139 ! dead animal

[Term]
id: MBTO:00000261
name: field
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00000076
name: bioreactor
is_a: MBTO:00000715 ! artificial environment

[Term]
id: MBTO:00000810
name: cultivated habitat
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00000366
name: dead matter
related_synonym: "dead organic matter" [TyDI:27237]
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00001038
name: Damselfish
exact_synonym: "Damselfishes" [TyDI:27240]
is_a: MBTO:00000561 ! fish

[Term]
id: MBTO:00001268
name: carcass
is_a: MBTO:00001824 ! organic matter

[Term]
id: MBTO:00001141
name: environment water
is_a: MBTO:00000643 ! aquatic environment

[Term]
id: MBTO:00001005
name: cyanobacterial mat
is_a: MBTO:00000034 ! microbial mat

[Term]
id: MBTO:00001430
name: black smoker chimney
is_a: MBTO:00000933 ! hydrothermal vent chimney

[Term]
id: MBTO:00001845
name: bay
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00000533
name: borehole
is_a: MBTO:00002008 ! extractive industry equipment

[Term]
id: MBTO:00000425
name: fracture water
is_a: MBTO:00001867 ! ground water

[Term]
id: MBTO:00000837
name: decaying leaf litter from a pine forest
is_a: MBTO:00001811 ! decaying leaf litter

[Term]
id: MBTO:00001811
name: decaying leaf litter
is_a: MBTO:00001794 ! decaying leaf

[Term]
id: MBTO:00001935
name: seagrass
is_a: MBTO:00000841 ! grass plant

[Term]
id: MBTO:00000446
name: decaying insect-invaded wood
is_a: MBTO:00001189 ! decaying matter

[Term]
id: MBTO:00001914
name: decaying bamboo leaf
is_a: MBTO:00001794 ! decaying leaf

[Term]
id: MBTO:00000853
name: decaying wood
is_a: MBTO:00001843 ! decaying plant material

[Term]
id: MBTO:00001853
name: decaying apple
is_a: MBTO:00001550 ! decaying fruit

[Term]
id: MBTO:00001794
name: decaying leaf
is_a: MBTO:00001843 ! decaying plant material

[Term]
id: MBTO:00000131
name: smooth cord grass
exact_synonym: "Spartina alterniflora" [TyDI:30535]
is_a: MBTO:00000841 ! grass plant

[Term]
id: MBTO:00001843
name: decaying plant material
related_synonym: "decaying plant tissue" [TyDI:27275]
related_synonym: "decaying plant" [TyDI:27276]
is_a: MBTO:00000050 ! plant material
is_a: MBTO:00001189 ! decaying matter

[Term]
id: MBTO:00001337
name: mollusc
is_a: MBTO:00001947 ! invertebrate species

[Term]
id: MBTO:00000887
name: light organ
exact_synonym: "light-emitting organ" [TyDI:27280]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001590
name: lesion
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001075
name: leg
is_a: MBTO:00000280 ! musculoskeletal system part

[Term]
id: MBTO:00001443
name: liver
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00000634
name: lining of the small intestine
is_a: MBTO:00000224 ! digestive tract part
is_a: MBTO:00001403 ! lining

[Term]
id: MBTO:00000449
name: squid
is_a: MBTO:00001329 ! cephalopod

[Term]
id: MBTO:00001329
name: cephalopod
is_a: MBTO:00000960 ! marine eukaryotic species
is_a: MBTO:00001337 ! mollusc

[Term]
id: MBTO:00000144
name: ulcer
related_synonym: "ulceration" [TyDI:27295]
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001722
name: lymphatic system
related_synonym: "lymphatic" [TyDI:27300]
exact_synonym: "lymphatics" [TyDI:27306]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001774
name: immune system
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000012
name: macrophage
is_a: MBTO:00001022 ! phagocyte

[Term]
id: MBTO:00001214
name: membrane bound organelle
is_a: MBTO:00000637 ! organelle

[Term]
id: MBTO:00000637
name: organelle
is_a: MBTO:00001285 ! cell part

[Term]
id: MBTO:00001983
name: midgut
related_synonym: "embryonic digestive tube" [TyDI:27313]
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000570
name: musculoskeletal system
related_synonym: "locomotor apparatus" [TyDI:27315]
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001856
name: lower respiratory tract
is_a: MBTO:00000124 ! respiratory tract

[Term]
id: MBTO:00000769
name: lymph node
is_a: MBTO:00000510 ! lymphatic system part

[Term]
id: MBTO:00001374
name: root
related_synonym: "root system" [TyDI:27322]
is_a: MBTO:00001121 ! plant organ

[Term]
id: MBTO:00001022
name: phagocyte
is_a: MBTO:00001852 ! cell

[Term]
id: MBTO:00000271
name: skin wound
is_a: MBTO:00001282 ! wound
is_a: MBTO:00001857 ! skin lesion

[Term]
id: MBTO:00001609
name: open skin wound
is_a: MBTO:00000271 ! skin wound

[Term]
id: MBTO:00000142
name: necropolis
is_a: MBTO:00001322 ! monument

[Term]
id: MBTO:00001427
name: tomb
is_a: MBTO:00001322 ! monument

[Term]
id: MBTO:00001090
name: mural painting
is_a: MBTO:00001322 ! monument

[Term]
id: MBTO:00000845
name: beer-bottling plant
is_a: MBTO:00000704 ! bottling factory

[Term]
id: MBTO:00001861
name: polar sea ice
is_a: MBTO:00001733 ! sea ice

[Term]
id: MBTO:00001733
name: sea ice
is_a: MBTO:00001827 ! cold temperature environment

[Term]
id: MBTO:00001477
name: chapel
is_a: MBTO:00001322 ! monument

[Term]
id: MBTO:00001318
name: membrane
is_a: MBTO:00001580 ! animal tissue

[Term]
id: MBTO:00001398
name: mucous membrane
exact_synonym: "mucus membrane" [TyDI:27351]
is_a: MBTO:00001318 ! membrane

[Term]
id: MBTO:00001403
name: lining
is_a: MBTO:00001580 ! animal tissue

[Term]
id: MBTO:00001648
name: mucosal surface
is_a: MBTO:00000752 ! mucosal tissue

[Term]
id: MBTO:00000589
name: manure compost
is_a: MBTO:00001474 ! compost

[Term]
id: MBTO:00000258
name: soil crust
is_a: MBTO:00000836 ! mineral matter

[Term]
id: MBTO:00001319
name: coastal fish farm
is_a: MBTO:00001452 ! fish farm

[Term]
id: MBTO:00001277
name: symbiosome
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00000023
name: plant part
is_a: MBTO:00001576 ! part of living organism

[Term]
id: MBTO:00001426
name: stem nodule
is_a: MBTO:00002066 ! plant nodule

[Term]
id: MBTO:00002066
name: plant nodule
is_a: MBTO:00001277 ! symbiosome

[Term]
id: MBTO:00001747
name: polluted seawater
is_a: MBTO:00000763 ! polluted environment
is_a: MBTO:00001481 ! marine water

[Term]
id: MBTO:00001668
name: crude-oil-contaminated seawater
is_a: MBTO:00001747 ! polluted seawater

[Term]
id: MBTO:00002009
name: seaweed
is_a: MBTO:00000242 ! algae

[Term]
id: MBTO:00000242
name: algae
is_a: MBTO:00001645 ! aquatic eukaryotic species

[Term]
id: MBTO:00001174
name: cattle-farm compost
is_a: MBTO:00000589 ! manure compost

[Term]
id: MBTO:00002010
name: caecal content
is_a: MBTO:00000341 ! feces

[Term]
id: MBTO:00001301
name: crude oil
related_synonym: "fossil fuel" [TyDI:27386]
exact_synonym: "petroleum" [TyDI:27387]
is_a: MBTO:00000296 ! oil

[Term]
id: MBTO:00001401
name: cotton-waste composts
is_a: MBTO:00001474 ! compost

[Term]
id: MBTO:00001968
name: soft tissue
is_a: MBTO:00001580 ! animal tissue

[Term]
id: MBTO:00000055
name: spleen
exact_synonym: "splenic" [TyDI:25510]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001390
name: splenic abcess
is_a: MBTO:00001078 ! abscess

[Term]
id: MBTO:00000512
name: stem
is_a: MBTO:00001121 ! plant organ

[Term]
id: MBTO:00000918
name: fermented tea leaves
is_a: MBTO:00000893 ! fermented vegetable food

[Term]
id: MBTO:00002024
name: dried seaweed
is_a: MBTO:00000751 ! plant-derived food

[Term]
id: MBTO:00001293
name: rumen
is_a: MBTO:00001035 ! ruminant digestive system part

[Term]
id: MBTO:00000132
name: PCE contaminated site
exact_synonym: "tetrachloroethene contaminated site" [TyDI:30193]
exact_synonym: "perchloroethylene contaminated site" [TyDI:30194]
is_a: MBTO:00000056 ! site contaminated with organic compound

[Term]
id: MBTO:00001665
name: chlorine-contaminated site
is_a: MBTO:00000056 ! site contaminated with organic compound

[Term]
id: MBTO:00001327
name: high sulfur concentration environment
is_a: MBTO:00000830 ! extreme environment

[Term]
id: MBTO:00001417
name: antibiotic-containing media
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00001941
name: high salt concentration environment
is_a: MBTO:00001346 ! haline environment

[Term]
id: MBTO:00000172
name: patient with cirrhosis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000849
name: groundwater body
is_a: MBTO:00001133 ! subterrestrial habitat
is_a: MBTO:00001378 ! inland water body

[Term]
id: MBTO:00001257
name: marine environment
related_synonym: "marine" [TyDI:24359]
related_synonym: "sea" [TyDI:24909]
related_synonym: "ocean" [TyDI:24998]
related_synonym: "marine area" [TyDI:24361]
related_synonym: "oceanic" [TyDI:24999]
is_a: MBTO:00000643 ! aquatic environment
xref: ENVO:00000016 ! sea

[Term]
id: MBTO:00001945
name: fuel oil piping system
is_a: MBTO:00000594 ! piping system

[Term]
id: MBTO:00000594
name: piping system
is_a: MBTO:00000578 ! industrial equipment

[Term]
id: MBTO:00000913
name: gas piping system
is_a: MBTO:00000594 ! piping system

[Term]
id: MBTO:00000566
name: water treatment plant
related_synonym: "water treatment facility" [TyDI:30217]
is_a: MBTO:00000535 ! artificial water structure

[Term]
id: MBTO:00000549
name: hydrocarbon
is_a: MBTO:00000122 ! organic compound

[Term]
id: MBTO:00000122
name: organic compound
is_a: MBTO:00000025 ! industrial chemical

[Term]
id: MBTO:00001535
name: home heating system
is_a: MBTO:00001295 ! domestic appliance

[Term]
id: MBTO:00000220
name: patient with malnutrition
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001527
name: patient with lymphoma
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001385
name: patient with granulomatous disease
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001230
name: cool soil
is_a: MBTO:00000458 ! soil with physical property

[Term]
id: MBTO:00001003
name: earth
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001751
name: human body
is_a: MBTO:00000190 ! body

[Term]
id: MBTO:00000260
name: liquid medium
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00001703
name: yeast extract
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00000628
name: palm oil
is_a: MBTO:00001222 ! oil

[Term]
id: MBTO:00000010
name: male cattle
is_a: MBTO:00000370 ! cattle

[Term]
id: MBTO:00001047
name: diagnostic equipment
is_a: MBTO:00001144 ! medical equipment

[Term]
id: MBTO:00000777
name: industrial product
is_a: MBTO:00000871 ! industrial habitat

[Term]
id: MBTO:00001171
name: medical product
is_a: MBTO:00000573 ! medical environment

[Term]
id: MBTO:00000613
name: road part
is_a: MBTO:00000987 ! constructed habitat

[Term]
id: MBTO:00000544
name: home drainage system
is_a: MBTO:00001295 ! domestic appliance

[Term]
id: MBTO:00001307
name: non-immune serum
exact_synonym: "nonimmune serum" [TyDI:30247]
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00000871
name: industrial habitat
is_a: MBTO:00000715 ! artificial environment

[Term]
id: MBTO:00000025
name: industrial chemical
is_a: MBTO:00000871 ! industrial habitat

[Term]
id: MBTO:00001679
name: water from air and water system
is_a: MBTO:00000339 ! industrial water and effluent

[Term]
id: MBTO:00001222
name: oil
is_a: MBTO:00001632 ! liquid food

[Term]
id: MBTO:00000909
name: starter culture
is_a: MBTO:00000181 ! microflora

[Term]
id: MBTO:00001823
name: sulfate-rich wastewater
is_a: MBTO:00000179 ! waste water

[Term]
id: MBTO:00000855
name: sauce
is_a: MBTO:00001632 ! liquid food

[Term]
id: MBTO:00001455
name: sugar cane juice
is_a: MBTO:00001870 ! juice

[Term]
id: MBTO:00000706
name: sugar cane field
is_a: MBTO:00001803 ! cultivated field

[Term]
id: MBTO:00000450
name: Medicago
is_a: MBTO:00000071 ! Leguminosae

[Term]
id: MBTO:00001288
name: waste water pipe
related_synonym: "waste pipe" [TyDI:30269]
is_a: MBTO:00000355 ! water pipe
is_a: MBTO:00001599 ! wastewater treatment equipment

[Term]
id: MBTO:00001948
name: patient treated with antibiotics
is_a: MBTO:00001633 ! patient treated by medication

[Term]
id: MBTO:00000647
name: river water
is_a: MBTO:00000862 ! flowing water

[Term]
id: MBTO:00000957
name: water cooling system
is_a: MBTO:00000535 ! artificial water structure

[Term]
id: MBTO:00001258
name: patient with pleuritis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001607
name: patient
related_synonym: "person with treated disease" [TyDI:30280]
exact_synonym: "treated patient" [TyDI:30281]
is_a: MBTO:00002067 ! ill person

[Term]
id: MBTO:00001076
name: patient with endocarditis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000016
name: patient with colitis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000140
name: patient with carcinoma
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000498
name: patient with tuberculosis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001884
name: patient with conjunctivitis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000883
name: patient with osteitis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000610
name: patient with hepatitis C
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000414
name: scientist
is_a: MBTO:00001055 ! worker

[Term]
id: MBTO:00000411
name: patient with cardiomyopathy
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001675
name: pediatric patient
is_a: MBTO:00000650 ! child
is_a: MBTO:00001607 ! patient

[Term]
id: MBTO:00000722
name: patient with periodontitis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00002032
name: patient with leukemia
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000864
name: patient with gastric ulcer
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001049
name: patient with keratitis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000452
name: patient with Crohn's disease
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000671
name: patient with bacteremia
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00002051
name: patient with Chron's disease
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000805
name: patient with urinary tract infection
exact_synonym: "patient with UTI" [TyDI:30317]
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001554
name: patient with lymphadenitis
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00001807
name: boy
is_a: MBTO:00000650 ! child

[Term]
id: MBTO:00001594
name: seabed
exact_synonym: "sea floor" [TyDI:27541]
is_a: MBTO:00001257 ! marine environment

[Term]
id: MBTO:00001036
name: dog tick
is_a: MBTO:00002058 ! hard tick

[Term]
id: MBTO:00000596
name: machinery
related_synonym: "machine" [TyDI:30326]
is_a: MBTO:00000578 ! industrial equipment

[Term]
id: MBTO:00002025
name: laboratory
is_a: MBTO:00001447 ! research and study center

[Term]
id: MBTO:00001046
name: inside the body
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001008
name: nutrient-poor soil
is_a: MBTO:00000961 ! soil with chemical property

[Term]
id: MBTO:00000996
name: back
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001563
name: polar stalk
is_a: MBTO:00001546 ! bacteria part

[Term]
id: MBTO:00001456
name: host associated biofilm
is_a: MBTO:00001805 ! biofilm

[Term]
id: MBTO:00000263
name: lumber
is_a: MBTO:00002031 ! wood

[Term]
id: MBTO:00000355
name: water pipe
is_a: MBTO:00001825 ! water transport structure
is_a: MBTO:00000594 ! piping system

[Term]
id: MBTO:00000928
name: aquatic sediment
is_a: MBTO:00000633 ! sediment

[Term]
id: MBTO:00001364
name: oil-water separator
is_a: MBTO:00002008 ! extractive industry equipment

[Term]
id: MBTO:00000814
name: oomycete
is_a: MBTO:00000593 ! eukaryote host

[Term]
id: MBTO:00000798
name: infant formula
is_a: MBTO:00000688 ! food

[Term]
id: MBTO:00001109
name: root surface
is_a: MBTO:00000771 ! root part

[Term]
id: MBTO:00001362
name: gingival lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00001916
name: patient with infectious disease
is_a: MBTO:00000985 ! patient with disease

[Term]
id: MBTO:00000195
name: paper pulp
is_a: MBTO:00000777 ! industrial product

[Term]
id: MBTO:00000674
name: biofilm reactor
is_a: MBTO:00000026 ! experimental medium

[Term]
id: MBTO:00001562
name: human filarial nematode
is_a: MBTO:00000002 ! filarial nematode

[Term]
id: MBTO:00001550
name: decaying fruit
is_a: MBTO:00001843 ! decaying plant material

[Term]
id: MBTO:00000959
name: area with epidemiologic property
is_a: MBTO:00000528 ! natural environment habitat

[Term]
id: MBTO:00000123
name: leaf vein
is_a: MBTO:00001121 ! plant organ

[Term]
id: MBTO:00000137
name: paper
is_a: MBTO:00000777 ! industrial product

[Term]
id: MBTO:00000818
name: adult animal
is_a: MBTO:00000191 ! animal with life stage property

[Term]
id: MBTO:00000337
name: silt
is_a: MBTO:00001166 ! soil matter

[Term]
id: MBTO:00001655
name: clay
is_a: MBTO:00001166 ! soil matter

[Term]
id: MBTO:00001475
name: guar
is_a: MBTO:00000379 ! terrestrial plant

[Term]
id: MBTO:00001204
name: sawmill
is_a: MBTO:00000407 ! industrial building

[Term]
id: MBTO:00000268
name: plant lesion
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001297
name: contaminated drinking water
is_a: MBTO:00000868 ! drinking water

[Term]
id: MBTO:00002061
name: mulberry
is_a: MBTO:00001465 ! fruit tree

[Term]
id: MBTO:00000278
name: plum tree
is_a: MBTO:00001465 ! fruit tree

[Term]
id: MBTO:00000675
name: vascular
is_a: MBTO:00000277 ! circulatory system part

[Term]
id: MBTO:00001749
name: elm
is_a: MBTO:00000772 ! tree

[Term]
id: MBTO:00000323
name: twig
is_a: MBTO:00001713 ! tree part

[Term]
id: MBTO:00000120
name: moth
is_a: MBTO:00000141 ! insect

[Term]
id: MBTO:00001767
name: parasitic nematode
is_a: MBTO:00002014 ! nematode

[Term]
id: MBTO:00000501
name: breeding site
is_a: MBTO:00000819 ! animal habitat

[Term]
id: MBTO:00000063
name: vaccine
is_a: MBTO:00001171 ! medical product

[Term]
id: MBTO:00001304
name: mitochondrion
is_a: MBTO:00000637 ! organelle

[Term]
id: MBTO:00001599
name: wastewater treatment equipment
is_a: MBTO:00000535 ! artificial water structure
is_a: MBTO:00001451 ! waste treatment equipment

[Term]
id: MBTO:00000189
name: biofilm in natural environment
is_a: MBTO:00001805 ! biofilm

[Term]
id: MBTO:00000277
name: circulatory system part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000280
name: musculoskeletal system part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001079
name: laboratory mice
is_a: MBTO:00001094 ! mouse
is_a: MBTO:00000098 ! laboratory animal

[Term]
id: MBTO:00000713
name: testis
exact_synonym: "testes" [TyDI:30428]
is_a: MBTO:00000644 ! urogenital tract part

[Term]
id: MBTO:00001898
name: mesentery
is_a: MBTO:00001318 ! membrane

[Term]
id: MBTO:00001479
name: nervous system
exact_synonym: "neurologic" [TyDI:25677]
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00000499
name: stratified marine water column
is_a: MBTO:00001481 ! marine water

[Term]
id: MBTO:00000064
name: water column
is_a: MBTO:00000824 ! lentic water

[Term]
id: MBTO:00000678
name: anoxic water
is_a: MBTO:00001547 ! anoxic environment
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00000127
name: leukocyte
is_a: MBTO:00001852 ! cell

[Term]
id: MBTO:00000750
name: excavation
is_a: MBTO:00001773 ! soil

[Term]
id: MBTO:00001991
name: planet
is_a: MBTO:00000872 ! bacteria habitat

[Term]
id: MBTO:00001644
name: earth
is_a: MBTO:00001991 ! planet

[Term]
id: MBTO:00000627
name: boar
is_a: MBTO:00001033 ! swine

[Term]
id: MBTO:00001248
name: circulatory system
is_a: MBTO:00000797 ! organ

[Term]
id: MBTO:00001604
name: artery
is_a: MBTO:00001974 ! blood vessel

[Term]
id: MBTO:00000175
name: foot
is_a: MBTO:00000280 ! musculoskeletal system part

[Term]
id: MBTO:00000084
name: breast
is_a: MBTO:00000693 ! mammalian part

[Term]
id: MBTO:00000934
name: atherosclerotic lesion
is_a: MBTO:00001590 ! lesion

[Term]
id: MBTO:00000161
name: aorta
is_a: MBTO:00001604 ! artery

[Term]
id: MBTO:00000020
name: HIV patient
is_a: MBTO:00001201 ! immunodeficient person
is_a: MBTO:00001607 ! patient

[Term]
id: MBTO:00000664
name: finger
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00000578
name: industrial equipment
is_a: MBTO:00000247 ! industrial site

[Term]
id: MBTO:00001434
name: environment wrt oxygen level
is_a: MBTO:00000114 ! habitat wrt chemico-physical property

[Term]
id: MBTO:00000056
name: site contaminated with organic compound
is_a: MBTO:00000946 ! contaminated site

[Term]
id: MBTO:00001065
name: olive oil
is_a: MBTO:00001222 ! oil

[Term]
id: MBTO:00001276
name: saline lake
is_a: MBTO:00000617 ! saline water
is_a: MBTO:00001370 ! lake

[Term]
id: MBTO:00000027
name: creek
is_a: MBTO:00002016 ! lotic water body

[Term]
id: MBTO:00001760
name: creek water
is_a: MBTO:00000862 ! flowing water

[Term]
id: MBTO:00000066
name: stream water
is_a: MBTO:00000862 ! flowing water

[Term]
id: MBTO:00001114
name: lentic water body
is_a: MBTO:00001378 ! inland water body

[Term]
id: MBTO:00002016
name: lotic water body
is_a: MBTO:00001378 ! inland water body

[Term]
id: MBTO:00000904
name: freshwater
exact_synonym: "fresh water" [TyDI:24751]
is_a: MBTO:00000952 ! environmental water with chemical property

[Term]
id: MBTO:00001378
name: inland water body
is_a: MBTO:00000643 ! aquatic environment

[Term]
id: MBTO:00001645
name: aquatic eukaryotic species
is_a: MBTO:00000593 ! eukaryote host

[Term]
id: MBTO:00000151
name: aquatic crustacean
is_a: MBTO:00001772 ! crustacean

[Term]
id: MBTO:00000693
name: mammalian part
is_a: MBTO:00000272 ! vertebrate part

[Term]
id: MBTO:00000454
name: primate part
is_a: MBTO:00000693 ! mammalian part

[Term]
id: MBTO:00000605
name: arachnid
is_a: MBTO:00000850 ! arthropod

[Term]
id: MBTO:00000755
name: deer tick
exact_synonym: "Ixodes scapularis" [TyDI:24053]
is_a: MBTO:00002058 ! hard tick

[Term]
id: MBTO:00001772
name: crustacean
is_a: MBTO:00000850 ! arthropod

[Term]
id: MBTO:00000191
name: animal with life stage property
is_a: MBTO:00000929 ! animal with age or sex property

[Term]
id: MBTO:00001224
name: drinking water filter
is_a: MBTO:00001911 ! drinking water facility

[Term]
id: MBTO:00000358
name: wood tick
exact_synonym: "Dermacentor variabilis" [TyDI:30508]
exact_synonym: "Ixodes ricinus" [TyDI:24051]
exact_synonym: "American dog tick" [TyDI:30509]
is_a: MBTO:00001118 ! tick

[Term]
id: MBTO:00002058
name: hard tick
is_a: MBTO:00001118 ! tick

[Term]
id: MBTO:00001043
name: alveolar epithelium
is_a: MBTO:00000164 ! respiratory tract part

[Term]
id: MBTO:00000420
name: plant tissue
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00001054
name: wood
is_a: MBTO:00001713 ! tree part

[Term]
id: MBTO:00001356
name: bark
is_a: MBTO:00001713 ! tree part

[Term]
id: MBTO:00001713
name: tree part
is_a: MBTO:00000023 ! plant part

[Term]
id: MBTO:00000489
name: aphotic zone
is_a: MBTO:00000955 ! environmental water with physical property

[Term]
id: MBTO:00001178
name: photic zone
related_synonym: "euphotic zone" [TyDI:30525]
related_synonym: "illuminated aquatic environment" [TyDI:30526]
is_a: MBTO:00000955 ! environmental water with physical property

[Term]
id: MBTO:00001101
name: illuminated anoxic zone of aquatic environment
is_a: MBTO:00001547 ! anoxic environment
is_a: MBTO:00001178 ! photic zone

[Term]
id: MBTO:00001035
name: ruminant digestive system part
is_a: MBTO:00000693 ! mammalian part

[Term]
id: MBTO:00000272
name: vertebrate part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001635
name: stratified water
is_a: MBTO:00000824 ! lentic water

[Term]
id: MBTO:00001634
name: rash
is_a: MBTO:00001209 ! skin part

[Term]
id: MBTO:00000419
name: sugar-beet
is_a: MBTO:00000851 ! tuber

[Term]
id: MBTO:00001670
name: gut
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000562
name: trichome
is_a: MBTO:00001805 ! biofilm

[Term]
id: MBTO:00000135
name: hoof
is_a: MBTO:00000693 ! mammalian part

[Term]
id: MBTO:00000607
name: agricultural species
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00001693
name: cultivated species
is_a: MBTO:00000607 ! agricultural species

[Term]
id: MBTO:00000359
name: recreational fishing fish pond
is_a: MBTO:00002056 ! fish pond

[Term]
id: MBTO:00001353
name: dairy starter culture
is_a: MBTO:00000909 ! starter culture

[Term]
id: MBTO:00001876
name: fish farming pond
is_a: MBTO:00000448 ! aquaculture pond
is_a: MBTO:00002056 ! fish pond

[Term]
id: MBTO:00000980
name: aquaculture farm
is_a: MBTO:00001352 ! aquaculture habitat

[Term]
id: MBTO:00000870
name: mushroom bed
is_a: MBTO:00000532 ! agricultural habitat

[Term]
id: MBTO:00000303
name: mushroom factory farm
exact_synonym: "mushroom farm" [TyDI:30644]
is_a: MBTO:00000380 ! farm

[Term]
id: MBTO:00001727
name: oil industry
is_a: MBTO:00001473 ! extractive industrial site

[Term]
id: MBTO:00001345
name: water well
is_a: MBTO:00001753 ! artificial water environment

[Term]
id: MBTO:00001957
name: dead organism
is_a: MBTO:00000366 ! dead matter

[Term]
id: MBTO:00000230
name: bone caries
is_a: MBTO:00002063 ! caries

[Term]
id: MBTO:00002060
name: trunk
is_a: MBTO:00001713 ! tree part

[Term]
id: MBTO:00001209
name: skin part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001697
name: skin bump
is_a: MBTO:00001209 ! skin part

[Term]
id: MBTO:00001877
name: skin nodule
is_a: MBTO:00001209 ! skin part

[Term]
id: MBTO:00000889
name: branch
is_a: MBTO:00001713 ! tree part

[Term]
id: MBTO:00001975
name: dental root
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00000484
name: heartwood
is_a: MBTO:00001054 ! wood

[Term]
id: MBTO:00000997
name: tongue
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00001981
name: beak
is_a: MBTO:00000866 ! mouth part

[Term]
id: MBTO:00001592
name: hospital humidifier
is_a: MBTO:00000455 ! hospital equipment
is_a: MBTO:00000825 ! humidifier

[Term]
id: MBTO:00000031
name: hospital water supply
is_a: MBTO:00000455 ! hospital equipment

[Term]
id: MBTO:00001633
name: patient treated by medication
related_synonym: "patient treated with medication" [TyDI:30690]
is_a: MBTO:00001607 ! patient

[Term]
id: MBTO:00000985
name: patient with disease
is_a: MBTO:00001607 ! patient

[Term]
id: MBTO:00001893
name: hospital tap water
is_a: MBTO:00000069 ! tap water
is_a: MBTO:00000169 ! bedside water bottle

[Term]
id: MBTO:00000352
name: hospital nebuliser
is_a: MBTO:00000455 ! hospital equipment

[Term]
id: MBTO:00000531
name: hospital drinking water
is_a: MBTO:00000217 ! hospital water
is_a: MBTO:00000868 ! drinking water

[Term]
id: MBTO:00000700
name: hot water distribution system
is_a: MBTO:00002030 ! hospital water distribution system

[Term]
id: MBTO:00000510
name: lymphatic system part
is_a: MBTO:00000395 ! animal part

[Term]
id: MBTO:00001432
name: hospital environment
is_a: MBTO:00000573 ! medical environment

[Term]
id: MBTO:00002030
name: hospital water distribution system
is_a: MBTO:00000031 ! hospital water supply

[Term]
id: MBTO:00000262
name: oesophagus
exact_synonym: "gullet" [TyDI:30708]
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000978
name: crop
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001251
name: caecum
exact_synonym: "cecum" [TyDI:30712]
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00000080
name: gizzard
is_a: MBTO:00000224 ! digestive tract part

[Term]
id: MBTO:00001259
name: reticulum
is_a: MBTO:00001035 ! ruminant digestive system part

[Term]
id: MBTO:00000584
name: fore-stomach
is_a: MBTO:00001035 ! ruminant digestive system part

[Term]
id: MBTO:00001936
name: omasum
is_a: MBTO:00001035 ! ruminant digestive system part

[Term]
id: MBTO:00000093
name: abomasum
is_a: MBTO:00001035 ! ruminant digestive system part

[Term]
id: MBTO:00001348
name: neonatal patient
is_a: MBTO:00000650 ! child
is_a: MBTO:00001607 ! patient

[Term]
id: MBTO:00001450
name: elderly patient
is_a: MBTO:00001522 ! adult human
is_a: MBTO:00001607 ! patient

[Term]
id: MBTO:00001181
name: pregnant patient
is_a: MBTO:00001607 ! patient
'''



BB1_SCHEMA = '''[Habitat]
kind = entity

[OntoBiotope]
kind = normalization
target = Habitat
referent = MBTO:\d+
'''


BB23_SCHEMA = '''[Habitat]
kind = entity

[Bacteria]
kind = entity

[Geographical]
kind = entity

[Localization]
kind = relation
arg.Bacterium = Bacteria
arg.Localization = Habitat, Geographical

[PartOf]
kind = relation
arg.Host = Habitat
arg.Part = Habitat
'''


class BB2_Score(NumericScore):
    def __init__(self):
        NumericScore.__init__(self)

    def score(self, doc1, doc2, a1, a2):
        if a1.is_equivalent(a2):
            return 1
        return 0

class BB3_Score(NumericScore):
    def __init__(self, ref1):
        NumericScore.__init__(self)
        self.ref1 = ref1
        self.jaccard = Jaccard()

    def _pairing_sets(self, doc1, doc2, a1, a2):
        if self.ref1:
            set2 = (a2,)
            if a1 in doc1.equivalence_sets:
                return (doc1.equivalence_sets[a1], set2)
            return ((a1,), set2)
        set1 = (a1,)
        if a2 in doc2.equivalence_sets:
            return (set1, doc2.equivalence_sets[a2])
        return (set1, (a2,))

    def _arg_pairing(self, doc1, doc2, a1, a2):
        set1, set2 = self._pairing_sets(doc1, doc2, a1, a2)
        pairing = self.jaccard.pairing(doc1, doc2, set1, set2)
        result = 0
        for _, score in pairing.itervalues():
            if score > result:
                result = score
        return result
    
    def score(self, doc1, doc2, a1, a2):
        is_part_of = (a1.type == 'PartOf')
        if is_part_of:
            if a2.type != 'PartOf':
                return 0
            host1 = a1.args['Host']
            host2 = a2.args['Host']
            host_score = self._arg_pairing(doc1, doc2, host1, host2)
            if host_score == 0:
                return 0
            part1 = a1.args['Part']
            part2 = a2.args['Part']
            part_score = self._arg_pairing(doc1, doc2, part1, part2)
            if part_score == 0:
                return 0
            return 1
        if a2.type == 'PartOf':
            return 0
        bact1 = a1.args['Bacterium']
        bact2 = a2.args['Bacterium']
        bact_score = self._arg_pairing(doc1, doc2, bact1, bact2)
        if bact_score < 1:
            bact_score = 0.000001
        loc1 = a1.args['Localization']
        loc2 = a2.args['Localization']
        return bact_score * self._arg_pairing(doc1, doc2, loc1, loc2)


class BioNLP_ST_BB(BioNLP_ST):
    def __init__(self):
        BioNLP_ST.__init__(self, 1)
        self.add_option('--task', action='store', type='int', dest='task', help='evaluated task (mandatory)')
        self.add_option('--pred-dir', action='store', type='string', dest='pred_dir', help='name of the directory containing prediction .a2 files')
        self.add_option('--verbose', action='store_true', dest='verbose', help='print detailed error analysis')

    def _parse_cmdline(self):
        BioNLP_ST._parse_cmdline(self)
        if self.options.task == 1:
            self.schema_file = StringIO(BB1_SCHEMA)
        elif self.options.task == 2:
            self.schema_file = StringIO(BB23_SCHEMA)
        elif self.options.task == 3:
            self.schema_file = StringIO(BB23_SCHEMA)
        elif self.options.task is None:
            raise Exception('--task option is mandatory')
        else:
            raise Exception('illegal value for --task')

    def run(self):
        BioNLP_ST.run(self)
        if self.options.pred_dir:
            self.prediction = self._load_corpus(self.options.pred_dir)
            if self.options.task == 1:
                self._evaluate1()
            elif self.options.task == 2:
                self._evaluate3()
            elif self.options.task == 3:
                self._evaluate3()

    def _evaluate1(self):
        ontology = Ontology()
        reader = OntologyReader(ontology)
        print 'Loading OntoBiotope_BioNLP-ST13.obo'
        reader.read('OntoBiotope_BioNLP-ST13.obo', StringIO(OBO), UnhandledTagFail(), DeprecatedTagSilent())
        ontology.check_required()
        ontology.resolve_references(DanglingReferenceFail())
        jaccard = Jaccard()
        wang = Wang_Entity(ontology, 0.65)
        evaluator = MultiplicativeNumericScore({'boundaries': jaccard, 'onto': wang})
        substitutions = 0.0
        insertions = 0
        deletions = 0
        total_ref = 0
        for ref_doc in self.corpus.documents:
            if self.options.verbose:
                stderr.write('document: %s\n' % ref_doc.id)
            pred_doc, = (d for d in self.prediction.documents if d.id == ref_doc.id)
            ref_ents = list(ref_doc.itertextbound())
            pred_ents = list(pred_doc.itertextbound())
            pairing = evaluator.pairing(ref_doc, pred_doc, ref_ents, pred_ents)
            seen_preds = set()
            total_ref += len(pairing)
            for ref, (pred, score0) in pairing.iteritems():
                if pred is None:
                    deletions += 1
                    if self.options.verbose:
                        stderr.write('    ' + ref.message('deletion\n'))
                    continue
                glob, score = score0
                seen_preds.add(pred)
                substitutions += 1 - glob
                if self.options.verbose and glob < 1:
                    stderr.write('    ' + ref.message(pred.message('mismatch (J = %f, W = %f, S = %f)\n' % (score['boundaries'], score['onto'], glob))))
        for pred in pred_ents:
            if pred not in seen_preds:
                insertions += 1
                stderr.write('    ' + pred.message('insertion\n'))
        print 'Substitutions =', substitutions
        print 'Insertions =', insertions
        print 'Deletions =', deletions
        print 'Reference =', total_ref
        print 'SER =', (substitutions + insertions + deletions) / total_ref

    def _evaluate3(self):
        recall_score = BB3_Score(True)
        precision_score = BB3_Score(False)
        n_ref = 0
        n_pred = 0
        recall_total = 0
        precision_total = 0
        for ref_doc in self.corpus.documents:
            pred_doc, = (d for d in self.prediction.documents if d.id == ref_doc.id)
            ref_rels = list(ref_doc.iterrelations())
            pred_rels = list(pred_doc.iterrelations())
            recall_pairing = recall_score.pairing(ref_doc, pred_doc, ref_rels, pred_rels)
            precision_pairing = precision_score.pairing(pred_doc, ref_doc, pred_rels, ref_rels)
            n_ref += len(ref_rels)
            n_pred += len(pred_rels)
            recall_local = 0
            precision_local = 0
            if self.options.verbose:
                stderr.write('document: %s\n' % ref_doc.id)
                stderr.write('    recall analysis:\n')
            for ref, (pred, score) in sorted(recall_pairing.iteritems()):
                if pred is None:
                    if self.options.verbose:
                        stderr.write('        %s: false negative\n' % ref.message())
                elif score <= 0.000001:
                    if self.options.verbose:
                        stderr.write('        %s: false negative (Bacteria has wrong boundaries)\n' % ref.message()) 
                else:
                    recall_local += score
                    if self.options.verbose:
                        if score != 1:
                            stderr.write('        %s: wrong boundaries (%s / %s)\n' % (ref.message(), ref.args['Localization'].message(), pred.args['Localization'].message()))
                        else:
                            pass
#                            stderr.write('        %s: true positive (%s)\n' % (ref.message(), pred.message()))
            if self.options.verbose:
                stderr.write('    precision analysis:\n')
            for pred, (ref, score) in sorted(precision_pairing.iteritems()):
                if ref is None:
                    if self.options.verbose:
                        stderr.write('        %s: false positive\n' % pred.message())
                elif score <= 0.000001:
                    if self.options.verbose:
                        stderr.write('        %s: false positive (Bacteria has wrong boundaries)\n' % pred.message())
                else:
                    precision_local += score
                    if score != 1 and self.options.verbose:
                        stderr.write('        %s: wrong boundaries (%s / %s)\n' % (ref.message(), ref.args['Localization'].message(), pred.args['Localization'].message()))
            recall_total += recall_local
            precision_total += precision_local
        recall = float(recall_total) / n_ref
        precision = float(precision_total) / n_pred
        print 'Recall = %f' % recall
        print 'Precision = %f' % precision
        print 'F1 = %f' % ((2 * recall * precision) / (recall + precision))

    def _evaluate2(self):
        trues = 0
        positives = 0
        true_positives = 0
        for ref_doc in self.corpus.documents:
            pred_doc, = (d for d in self.prediction.documents if d.id == ref_doc.id)
            entity_pairing = self._get_entity_pairing2(ref_doc, pred_doc)
            for rel in pred_doc.iterrelations():
                self._shift_predicted_relation2(entity_pairing, rel)
            ref2pred, pred2ref = self._evaluate_doc2(entity_pairing, ref_doc, pred_doc)
            trues += len(ref2pred)
            positives += len(pred2ref)
            if self.options.verbose:
                stderr.write('document: %s\n' % ref_doc.id)
                stderr.write('    recall analysis:\n')
            for r, (p, s) in sorted(ref2pred.iteritems()):
                if s == 1:
                    true_positives += 1
                elif self.options.verbose:
                    stderr.write('        %s: false negative\n' % r.message())
            if self.options.verbose:
                stderr.write('    precision analysis:\n')
                for p, (r, s) in sorted(pred2ref.iteritems()):
                    if s != 1:
                        stderr.write('        %s: false positive\n' % p.message())
        recall = float(true_positives) / float(trues)
        if positives > 0:
            precision = float(true_positives) / float(positives)
        else:
            precision = -1
        print 'Recall = ' + str(recall)
        print 'Precision = ' + str(precision)
        print 'F1 = ' + str((2 * recall * precision) / (recall + precision))

    def _get_entity_pairing2(self, ref_doc, pred_doc):
        return dict((tb, ref_doc.annotations[tb.id]) for tb in pred_doc.itertextbound())

    def _shift_predicted_relation2(self, entity_pairing, rel):
        rel.original_args = rel.args
        rel.args = dict((role, entity_pairing[arg]) for (role, arg) in rel.original_args.iteritems())

    def _evaluate_doc2(self, entity_pairing, ref_doc, pred_doc):
        ref_rels = self._get_non_redundant_relations2(ref_doc, warn=False)
        pred_rels = self._get_non_redundant_relations2(pred_doc)
        score = BB2_Score()
        ref2pred = score.pairing(ref_doc, pred_doc, ref_rels, pred_rels)
        pred2ref = score.pairing(pred_doc, ref_doc, pred_rels, ref_rels)
        return ref2pred, pred2ref
        
    def _get_non_redundant_relations2(self, doc, warn=True):
        result = []
        for rel in doc.iterrelations():
            equiv = tuple(seen for seen in result if rel.is_equivalent(seen))
            if equiv:
                if warn:
                    stderr.write(rel.message('equivalent to ' + equiv[0].message('')) + '\n')
            else:
                result.append(rel)
        return result

if __name__ == '__main__':
    BioNLP_ST_BB().run()
