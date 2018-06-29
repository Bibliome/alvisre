#!/usr/bin/env python


# Copyright (c) 2013, Institut National de la Recherche Agronomique (INRA)
# All rights reserved.

# Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

#     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#     Neither the names of the Institut National de la Recherche Agronomique (INRA) and BioNLP-ST 2013 nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


import re
from sys import stderr, exit
from optparse import OptionParser
from ConfigParser import ConfigParser


class Document:
    def __init__(self, source, corpus, id, text):
        self.source = source
        self.corpus = corpus
        self.id = id
        self.text = text
        self.annotations = {}
        self.equivalences = []
        corpus.documents.append(self)

    def copy_input(self, corpus):
        doc = Document(self.source, corpus, self.id, self.text)
        for a in self.annotations.itervalues():
            a.copy_input(doc)
        for e in self.equivalences:
            e.copy_input(doc)

    def resolve_ids(self):
        for a in self.annotations.values():
            a.resolve_ids()
        self.equivalence_sets = {}
        for e in self.equivalences:
            e.resolve_ids()
            eqset = set(e.annotations)
            for a in e.annotations:
                self.equivalence_sets[a] = eqset
        for a in self.annotations.itervalues():
            if a not in self.equivalence_sets:
                self.equivalence_sets[a] = set((a,))

    def iterannotations(self):
        return self.annotations.itervalues()

    def itertextbound(self):
        for a in self.annotations.itervalues():
            if isinstance(a, TextBound):
                yield a

    def iternormalizations(self, target=None):
        for a in self.iterannotations():
            if isinstance(a, Normalization) and (target is None or target == a.annotation):
                yield a

    def iterrelations(self):
        for a in self.annotations.itervalues():
            if isinstance(a, Relation):
                yield a

    def renum(self):
        next = {
            TextBound: 1,
            Event: 1,
            Modification: 1,
            Relation: 1,
            Normalization: 1
            }
        for a in self.annotations.itervalues():
            t = type(a)
            a.id = next[t]
            next[t] += 1


class Visibility:
    def __init__(self, face):
        self.face = face

    def __str__(self):
        return self.face


A1 = Visibility('input')
A2 = Visibility('reference')

class Sourced:
    def __init__(self, source, lineno, doc, visibility):
        self.source = source
        self.lineno = lineno
        self.doc = doc
        self.visibility = visibility

    def message(self, msg=None):
        if msg is None:
            return self.source + ':' + str(self.lineno)
        return self.source + ':' + str(self.lineno) + ': ' + msg

    def _resolve_id(self, id):
        if not id in self.doc.annotations:
            raise Exception(self.message('unknown id ' + id))
        return self.doc.annotations[id]

    def resolve_ids(self):
        raise NotImplementedError


class Annotation(Sourced):
    def __init__(self, source, lineno, doc, visibility, id, expected_idspace, type):
        Sourced.__init__(self, source, lineno, doc, visibility)
        self.id = id
        self.type = type
        if id[0] != expected_idspace:
            stderr.write(self.message('id should start with a \'' + expected_idspace + '\'\n'))
        if self.id in doc.annotations:
            raise Exception(self.message('duplicate identifier, see ' + doc.annotations[self.id].message('...')))
        doc.annotations[self.id] = self
        self.normalizations = []

    def arg_of(self, role=None):
        for rel in self.doc.annotations.itervalues():
            if isinstance(rel, AnnotationWithArgs) and rel.has_arg(self, role):
                yield rel

    def trigger_of(self):
        for ev in self.doc.annotations.itervalues():
            if isinstance(ev, Event) and ev.trigger == self:
                yield ev

    def get_norms(self, type=None):
        for n in self.doc.annotations.itervalues():
            if isinstance(n, Normalization) and (type is None or n.type == type) and n.annotation == self:
                yield n

    def get_mods(self, type=None):
        for m in self.doc.annotations.itervalues():
            if isinstance(m, Modification) and (type is None or m.type == type) and m.event == self:
                yield m

    def _prestr(self, *args):
        return self.__class__.__name__ + ':' + self.type + '/' + self.id + ':' + ''.join(args)

    def is_equivalent(self, other):
        return other in self.doc.equivalence_sets[self]


class TextBound(Annotation):
    RE = re.compile('(?P<id>[A-Z]\d+)\t(?P<type>.+) (?P<boundaries>\d+ \d+(?:;\d+ \d+)*)\t(?P<form>.*)$')
    
    def __init__(self, source, lineno, doc, visibility, id, type, boundaries, form):
        Annotation.__init__(self, source, lineno, doc, visibility, id, 'T', type)
        self.boundaries = tuple(sorted(boundaries))
        self.form = form
        l = len(doc.text)
        frags = []
        for s, e in boundaries:
            if s < 0:
                raise Exception(self.message('illegal start boundary' + str(s)))
            if e < 0:
                raise Exception(self.message('illegal end boundary' + str(e)))
            if s >= l:
                raise Exception(self.message('illegal start boundary' + str(s)))
            if e > l:
                raise Exception(self.message('illegal end boundary' + str(e)))
            frags.append(doc.text[s:e])
        expected_form = ' '.join(frags)
        if form != expected_form:
            raise Exception(self.message('failed surface form control, got "' + form + '", expected "' + expected_form + '"'))

    def copy_input(self, doc):
        if self.visibility is not A1:
            return
        t = TextBound(self.source, self.lineno, doc, self.visibility, self.id, self.type, self.boundaries, self.form)

    def resolve_ids(self):
        pass

    def __str__(self):
        return self._prestr('(', self.form, ')')

    def __len__(self):
        r = 0
        for start, end in self.boundaries:
            r += (end - start)
        return r


class AnnotationWithArgs(Annotation):
    def __init__(self, source, lineno, doc, visibility, id, expected_idspace, type, arg_ids):
        Annotation.__init__(self, source, lineno, doc, visibility, id, expected_idspace, type)
        self.arg_ids = dict(arg_ids)

    def resolve_ids(self):
        self.args = {}
        for role, arg_id in self.arg_ids.iteritems():
            self.args[role] = self._resolve_id(arg_id)

    def has_arg(self, arg, role=None):
        if role is None:
            return arg in (self.args.itervalues())
        return self.args[role] == arg

    def _argstr(self):
        return '{' + ', '.join(r + ': ' + str(a) for r,a in self.args.iteritems()) + '}'

    def is_equivalent(self, other):
        if Annotation.is_equivalent(self, other):
            return True
        if not isinstance(other, AnnotationWithArgs):
            return False
        if self.type != other.type:
            return False
        for role, arg in self.args.iteritems():
            if role not in other.args:
                return False
            if not arg.is_equivalent(other.args[role]):
                return False
        return True


class Event(AnnotationWithArgs):
    RE = re.compile('(?P<id>[A-Z]\d+)\t(?P<type>[^:\s]+):(?P<trigger>[A-Z]\d+)[ ]*(?P<args>(?: .*:[A-Z]\d+)*)$')
    
    def __init__(self, source, lineno, doc, visibility, id, type, arg_ids, trigger_id):
        AnnotationWithArgs.__init__(self, source, lineno, doc, visibility, id, 'E', type, arg_ids)
        self.trigger_id = trigger_id

    def copy_input(self, doc):
        if self.visibility is not A1:
            return
        Event(self.source, self.lineno, doc, self.visibility, self.id, self.type, self.arg_ids, self.trigger_id)

    def resolve_ids(self):
        AnnotationWithArgs.resolve_ids(self)
        self.trigger = self._resolve_id(self.trigger_id)

    def __str__(self):
        return self._prestr(str(self.trigger), '->', self._argstr())

    def is_equivalent(self, other):
        if not AnnotationWithArgs.is_equivalent(self, other):
            return False
        if not isinstance(other, Event):
            return False
        return self.trigger.is_equivalent(other.trigger)
    

class Modification(Annotation):
    RE = re.compile('(?P<id>[A-Z]\d+)\t(?P<type>.+) (?P<event>[A-Z]\d+)$')
    
    def __init__(self, source, lineno, doc, visibility, id, type, event_id):
        Annotation.__init__(self, source, lineno, doc, visibility, id, 'M', type)
        self.event_id = event_id

    def resolve_ids(self):
        self.event = self._resolve_id(self.event_id)

    def __str__(self):
        return self._prestr(str(self.event))
        

class Relation(AnnotationWithArgs):
    RE = re.compile('(?P<id>[A-Z]\d+)\t(?P<type>\S+)(?P<args>(?: .*:[A-Z]\d+)*)$')
    
    def __init__(self, source, lineno, doc, visibility, id, type, arg_ids):
        AnnotationWithArgs.__init__(self, source, lineno, doc, visibility, id, 'R', type, arg_ids)

    def __str__(self):
        return self._prestr(self._argstr())

    def copy_input(self, doc):
        if self.visibility is not A1:
            return
        Relation(self.source, self.lineno, doc, self.visibility, self.id, self.type, self.arg_ids)


class Normalization(Annotation):
    RE = re.compile('(?P<id>[A-Z]\d+)\t(?P<type>.+) Annotation:(?P<annotation>[A-Z]\d+) Referent:(?P<referent>.*)$')
    
    def __init__(self, source, lineno, doc, visibility, id, type, annotation_id, referent):
        Annotation.__init__(self, source, lineno, doc, visibility, id, 'N', type)
        self.annotation_id = annotation_id
        self.referent = referent

    def copy_input(self, doc):
        if self.visibility is not A1:
            return
        Normalization(self.source, self.lineno, doc, self.visibility, self.id, self.annotation_id, self.referent)
        
    def resolve_ids(self):
        self.annotation = self._resolve_id(self.annotation_id)
        self.annotation.normalizations.append(self)

    def __str__(self):
        return self._prestr(str(self.annotation), '->', self.referent)


class Equivalence(Sourced):
    RE = re.compile('\*\tEquiv(?P<annotations>(?: [A-Z]\d+)+)$')

    def __init__(self, source, lineno, doc, visibility, annotation_ids):
        Sourced.__init__(self, source, lineno, doc, visibility)
        self.annotation_ids = tuple(annotation_ids)
        doc.equivalences.append(self)

    def resolve_ids(self):
        self.annotations = tuple(self._resolve_id(a_id) for a_id in self.annotation_ids)

    def copy_input(self, doc):
        if self.visibility is not A1:
            return
        Equivalence(self.source, self.lineno, doc, self.visibility, self.annotation_ids)


class Corpus:
    def __init__(self):
        self.documents = []

    def copy_input(self):
        result = Corpus()
        for doc in self.documents:
            doc.copy_input(corpus)

    def _parse_args(self, s):
        result = {}
        s = s.strip()
        if s != '':
            for a in s.strip().split(' '):
                (role, arg_id) = a.split(':', 1)
                result[role] = arg_id
        return result

    def _parse_boundaries(self, s):
        result = []
        for b in s.strip().split(';'):
            (start, end) = b.split(' ')
            result.append((int(start), int(end)))
        return result

    def _parse_annotation_line(self, source, lineno, doc, visibility, line):
        r = Equivalence.RE.match(line)
        if r is not None:
            (annotation_ids) = r.group('annotations')
            return Equivalence(source, lineno, doc, visibility, annotation_ids.strip().split(' '))

        r = TextBound.RE.match(line)
        if r is not None:
            (id, type, boundaries, form) = r.group('id', 'type', 'boundaries', 'form')
            return TextBound(source, lineno, doc, visibility, id, type, self._parse_boundaries(boundaries), form)

        r = Normalization.RE.match(line)
        if r is not None:
            (id, type, annotation_id, referent) = r.group('id', 'type', 'annotation', 'referent')
            return Normalization(source, lineno, doc, visibility, id, type, annotation_id, referent)

        r = Modification.RE.match(line)
        if r is not None:
            (id, type, event_id) = r.group('id', 'type', 'event')
            return Modification(source, lineno, doc, visibility, id, type, event_id)

        r = Event.RE.match(line)
        if r is not None:
            (id, type, trigger_id, args) = r.group('id', 'type', 'trigger', 'args')
            return Event(source, lineno, doc, visibility, id, type, self._parse_args(args), trigger_id)

        r = Relation.RE.match(line)
        if r is not None:
            (id, type, args) = r.group('id', 'type', 'args')
            return Relation(source, lineno, doc, visibility, id, type, self._parse_args(args))

        unk = Annotation(source, lineno, doc, visibility, 'UNKNOWN', 'U', 'UNKNOWN')
        raise Exception(unk.message('could not parse annotation'))

    def _parse_annotation_file(self, source, doc, visibility):
        f = open(source)
        for lineno, line in enumerate(f):
            self._parse_annotation_line(source, lineno+1, doc, visibility, line)
        f.close()

    def iterdocs(self):
        return iter(self.documents)

    def iterannotations(self):
        for doc in self.iterdocs():
            for a in doc.iterannotations():
                yield a

    def parse_file_triplet(self, text_source, a1_dir=None, a2_dir=None):
        (dir, basename) = text_source.rsplit('/', 1)
        (doc_id, ext) = basename.rsplit('.', 1)
        if ext != 'txt':
            raise Exception('expected .txt file')
        f = open(text_source)
        text = f.read()
        f.close()
        doc = Document(text_source, self, doc_id, text)

        if a1_dir is not None:
            source = a1_dir + '/' + doc_id + '.a1'
            self._parse_annotation_file(source, doc, A1)

        if a2_dir is not None:
            source = a2_dir + '/' + doc_id + '.a2'
            self._parse_annotation_file(source, doc, A2)

        return doc

    def resolve_ids(self):
        for doc in self.documents:
            doc.resolve_ids()


class AnnotationSchema:
    def __init__(self, schema, annotation_class, type):
        self.schema = schema
        self.annotation_class = annotation_class
        self.type = type
        schema.annotation_schemas[type] = self

    def _get_value(self, key):
        if not self.schema.config.has_option(self.type, key):
            raise Exception(self.schema.message(self.type, 'missing \'' + key + '\''))
        return self.schema.config.get(self.type, key)
    
    def _get_values(self, key):
        return set(v.strip() for v in self._get_value(key).split(','))

    def check(self, annotation):
        if not isinstance(annotation, self.annotation_class):
            yield annotation.message('is ' + str(type(annotation)) + ', expected ' + str(self.annotation_class))

    def _check_type_of(self, annotation, target, allowed, target_form):
        if target.type not in allowed:
            yield annotation.message(target_form + ' has type ' + target.type + ', expected: ' + ', '.join(allowed))

    def _metacheck_types(self, types):
        for type in types:
            if type == '':
                raise Exception(self.schema.message(self.type, 'trailing comma'))
            if type not in self.schema.annotation_schemas:
                raise Exception(self.schema.message(self.type, 'unspecified annotation type ' + type))

    def _metacheck(self):
        pass


class TextBoundSchema(AnnotationSchema):
    def __init__(self, schema, type):
        AnnotationSchema.__init__(self, schema, TextBound, type)


class AnnotationWithArgsSchema(AnnotationSchema):
    def __init__(self, schema, annotation_class, type):
        AnnotationSchema.__init__(self, schema, annotation_class, type)
        self.args = {}
        for opt in self.schema.config.options(self.type):
            if not opt.startswith('arg.'):
                continue
            self.args[opt[4:]] = self._get_values(opt)

    def _metacheck(self):
        AnnotationSchema._metacheck(self)
        for at in self.args.itervalues():
            self._metacheck_types(at)

    def check(self, annotation):
        for msg in AnnotationSchema.check(self, annotation):
            yield msg
        for a in self.args:
            if a not in annotation.args:
                yield annotation.message('missing argument ' + a)
        for k, v in annotation.args.iteritems():
            if k not in self.args:
                yield annotation.message('unknown argument ' + k)
            else:
                for msg in self._check_type_of(annotation, v, self.args[k], annotation.type + '.' + k):
                    yield msg


class EventSchema(AnnotationWithArgsSchema):
    def __init__(self, schema, type):
        AnnotationWithArgsSchema.__init__(self, schema, Event, type)
        self.trigger = self._get_values('trigger')

    def _metacheck(self):
        AnnotationWithArgsSchema._metacheck(self)
        self._metacheck_types(self.trigger)
        
    def check(self, annotation):
        for msg in AnnotationWithArgsSchema.check(self, annotation):
            yield msg
        for msg in self._check_type_of(annotation, annotation.trigger, self.trigger, 'trigger'):
            yield msg


class ModificationSchema(AnnotationSchema):
    def __init__(self, schema, type):
        AnnotationSchema.__init__(self, schema, Modification, type)
        self.target = self._get_values('target')

    def check(self, annotation):
        for msg in AnnotationSchema.check(self, annotation):
            yield msg
        for msg in self._check_type_of(annotation, annotation.event, self.target, 'target'):
            yield msg

    def _metacheck(self):
        AnnotationSchema._metacheck(self)
        self._metacheck_types(self.target)
        

class RelationSchema(AnnotationWithArgsSchema):
    def __init__(self, schema, type):
        AnnotationWithArgsSchema.__init__(self, schema, Relation, type)

    def check(self, annotation):
        for msg in AnnotationSchema.check(self, annotation):
            yield msg
        for msg in AnnotationWithArgsSchema.check(self, annotation):
            yield msg

    def _metacheck(self):
        AnnotationWithArgsSchema._metacheck(self)


class NormalizationSchema(AnnotationSchema):
    def __init__(self, schema, type):
        AnnotationSchema.__init__(self, schema, Normalization, type)
        self.target = self._get_values('target')
        self.referent = re.compile(self._get_value('referent'))
        
    def check(self, annotation):
        for msg in AnnotationSchema.check(self, annotation):
            yield msg
        for msg in self._check_type_of(annotation, annotation.annotation, self.target, 'target'):
            yield msg
        if self.referent.match(annotation.referent) is None:
            yield annotation.message('referent form is incorrect: ' + annotation.referent)

    def _metacheck(self):
        AnnotationSchema._metacheck(self)
        self._metacheck_types(self.target)


class Schema:
    KINDS = {
        'entity': TextBoundSchema,
        'event': EventSchema,
        'modification': ModificationSchema,
        'relation': RelationSchema,
        'normalization': NormalizationSchema
        }
    
    def __init__(self, source, file=None):
        self.annotation_schemas = {}
        self.source = source
        self.config = ConfigParser()
        self.config.optionxform = str
        if file is not None:
            self.config.readfp(file, source)
        else:
            self.config.read(source)
        for type in self.config.sections():
            aschema = self._get_annotation_schema(type)
            aschema(self, type)
        for aschema in self.annotation_schemas.itervalues():
            aschema._metacheck()
    
    def message(self, type, message):
        return 'in ' + self.source + ', for type ' + type + ': ' + message

    def _get_annotation_schema(self, type):
        if not self.config.has_option(type, 'kind'):
            raise Exception(message(type, 'missing \'kind\''))
        kind = self.config.get(type, 'kind')
        if kind in Schema.KINDS:
            return Schema.KINDS[kind]
        raise Exception(message(type, 'unknown annotation kind ' + kind))

    def _check_annotation(self, annotation):
        t = annotation.type
        if t not in self.annotation_schemas:
            yield annotation.message('unknown annotation type ' + t)
        else:
            for msg in self.annotation_schemas[t].check(annotation):
                yield msg

    def _check_document(self, doc):
        for a in doc.annotations.itervalues():
            for msg in self._check_annotation(a):
                yield msg

    def check_corpus(self, corpus):
        for doc in corpus.documents:
            for msg in self._check_document(doc):
                yield msg





class BioNLP_ST(OptionParser):
    def __init__(self, schema_file=None):
        OptionParser.__init__(self, usage='usage: %prog [options] TXTFILES...')
        self.add_option('--a1-dir', action='store', type='string', dest='a1_dir', help='name of the directory containing .a1 files')
        self.add_option('--a2-dir', action='store', type='string', dest='a2_dir', help='name of the directory containing reference .a2 files')
        self.add_option('--silence', action='store', type='string', dest='ignore', help='if this option is given, then the schema error messages that match exactly one of the specified lines will be silenced')
        self.schema_file = schema_file
        if not schema_file:
            self.add_option('--schema', action='store', type='string', dest='schema', help='annotations will be checked against the specified schema')

    def optionxform(self, option):
        return option

    def _parse_cmdline(self):
        (self.options, self.args) = self.parse_args()

    def _load_corpus(self, a2_dir):
        result = Corpus()
        print 'Reading corpus'
        for f in self.args:
            result.parse_file_triplet(f, a1_dir=self.options.a1_dir, a2_dir=a2_dir)
        result.resolve_ids()
        return result

    def _validate_schema(self, corpus):
        if self.schema_file is None:
            if self.options.schema is None:
                return True
            self.schema = Schema(self.options.schema)
        else:
            self.schema = Schema('<internal>', self.schema_file)
        print 'Schema validation:'
        ignore = set()
        if self.options.ignore is not None:
            f = open(self.options.ignore)
            for l in f:
                ignore.add(l.strip())
            f.close()
        nerr = 0
        nsil = 0
        for msg in self.schema.check_corpus(corpus):
            nerr += 1
            if msg in ignore:
                nsil += 1
            else:
                stderr.write(msg + '\n')
        if nerr == 0:
            print '    ok'
            return True
        if self.options.ignore is not None:
            if nerr == nsil:
                print '   ', str(nerr),'errors (all silenced)'
                return True
            print '   ', str(nerr), 'errors (', str(nsil), 'silenced)'
            return False
        print '   ', str(nerr), 'errors'
        return False

    def run(self):
        self._parse_cmdline()
        self.corpus = self._load_corpus(self.options.a2_dir)
        if not self._validate_schema(self.corpus):
            exit(1)

if __name__ == '__main__':
    BioNLP_ST().run()
