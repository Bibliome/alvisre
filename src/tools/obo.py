#!/usr/bin/env python



# Copyright (c) 2013, Institut National de la Recherche Agronomique (INRA)
# All rights reserved.

# Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

#     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#     Neither the names of the Institut National de la Recherche Agronomique (INRA) and BioNLP-ST 2013 nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import re
from sys import stdin, stdout, stderr
from StringIO import StringIO
from time import strftime
from os import getenv


class OBOException(Exception):
    def __init__(self, sourced, msg):
        Exception.__init__(self, sourced.message(msg))

class OBONotImplemented(OBOException):
    def __init__(self, sourced, tag):
        OBOException.__init__(self, sourced, tag + ' not implemented yet')

class OBOInvalidFormat(OBOException):
    def __init__(self, sourced, tag):
        OBOException.__init__(self, sourced, 'invalid ' + tag + ' format')


class UnhandledTagOption:
    def __init__(self):
        pass

    def handle(self, tagset, tag, value):
        raise NotImplemented()

class UnhandledTagFail(UnhandledTagOption):
    def __init__(self):
        UnhandledTagOption.__init__(self)

    def handle(self, tagset, tag, value):
        raise OBOException(value.message('unhandled tag ' + tag))

class UnhandledTagWarn(UnhandledTagOption):
    def __init__(self):
        UnhandledTagOption.__init__(self)

    def handle(self, tagset, tag, value):
        value.warning('unhandled tag ' + tag)

class UnhandledTagRecord(UnhandledTagOption):
    def __init__(self):
        UnhandledTagOption.__init__(self)

    def handle(self, tagset, tag, value):
        tagset.unhandled_tags.append((tag, value))

class UnhadledTagWarnAndRecord(UnhandledTagWarn, UnhandledTagRecord):
    def __init__(self):
        UnhandledTagWarn.__init__(self)
        UnhandledTagRecord.__init__(self)

    def handle(self, tagset, tag, value):
        UnhandledTagWarn.handle(self, tagset, tag, value)
        UnhandledTagRecord.handle(self, tagset, tag, value)

class UnhandledTagIgnore(UnhandledTagOption):
    def __init__(self):
        UnhandledTagOption.__init__(self)

    def handle(self, tagset, tag, value):
        pass


class DeprecatedTagOption:
    def __init__(self):
        pass

    def handle(self, tagset, tag, value):
        raise NotImplemented()

class DeprecatedTagWarn(DeprecatedTagOption):
    def __init__(self):
        DeprecatedTagOption.__init__(self)

    def handle(self, tagset, tag, value):
        value.warning('deprecated tag: ' + tag)

class DeprecatedTagSilent(DeprecatedTagOption):
    def __init__(self):
        DeprecatedTagOption.__init__(self)

    def handle(self, tagset, tag, value):
        pass
    

class TagReader:
    def __init__(self, tagset, ontology, unhandled_tag_option, deprecated_tags_option):
        self.tagset = tagset
        self.ontology = ontology
        self.tags = []
        self.unhandled_tag_option = unhandled_tag_option
        self.deprecated_tags_option = deprecated_tags_option

    def read(self, tag, value):
        method_name = 'read_' + tag.replace('-', '_')
        if hasattr(self, method_name):
            getattr(self, method_name)(value)
        else:
            self.default_read(tag, value)
    
    def default_read(self, tag, value):
        value.warning('unhandled tag ' + tag + ' in ' + self.tagset.__class__.__name__)



def unquoted_string(name):
    return r'(?P<' + name + r'>(?:\\.|[^ \[\]])+)'

def quoted_string(name):
    return r'"(?P<' + name + '>(?:[^\"]|\\.)+)"'

TERMINAL_COMMENT = r'\s*(?:!.*)?$'
SCOPE = r'(?:\s+(?P<scope>EXACT|BROAD|NARROW|RELATED))?'
DBXREF_LIST = r'(?:\s+\[(?:[^ \[\]]|\\.)*\])?'

DATE_VALUE_PATTERN = re.compile(r'(?P<date>\d\d:\d\d:\d\d\d\d \d\d:\d\d)' + TERMINAL_COMMENT)
SUBSETDEF_PATTERN = re.compile(unquoted_string('subset') + '\s+' + quoted_string('descr') + TERMINAL_COMMENT)
SYNONYMTYPEDEF_PATTERN = re.compile(unquoted_string('name') + '\s+' + quoted_string('descr') + SCOPE + TERMINAL_COMMENT)
FREE_VALUE_PATTERN = re.compile(r'(?P<value>(?:\\.|[^!\[\]])+)' + DBXREF_LIST + TERMINAL_COMMENT)
BOOLEAN_VALUE_PATTERN = re.compile(r'(?P<value>true|false)' + TERMINAL_COMMENT)
QUOTED_VALUE_PATTERN = re.compile(quoted_string('value') + TERMINAL_COMMENT)
SYNONYM_PATTERN = re.compile(quoted_string('text') + SCOPE + r'(?: ' + unquoted_string('type') + ')?' + DBXREF_LIST + TERMINAL_COMMENT)
DEPRECATED_SYNONYM_PATTERN = re.compile(quoted_string('text') + r'(?: ' + unquoted_string('type') + ')?' + DBXREF_LIST + TERMINAL_COMMENT)
XREF_PATTERN = re.compile(unquoted_string('id') + r'(?: ' + quoted_string('descr') + r')?' + TERMINAL_COMMENT)
INTERSECTION_PATTERN = re.compile('(?:' + unquoted_string('rel') + ' )?' + unquoted_string('id') + TERMINAL_COMMENT)
RELATIONSHIP_PATTERN = re.compile(unquoted_string('rel') + '\s+' + unquoted_string('id') + TERMINAL_COMMENT)
INSTANCE_PROPERTY_VALUE_PATTERN = re.compile(unquoted_string('rel') + '(?: ' + quoted_string('value') + ')?' + '\s+' + unquoted_string('ref') + TERMINAL_COMMENT)
DEFINITION_PATTERN = re.compile(quoted_string('definition') + DBXREF_LIST + TERMINAL_COMMENT)

def match_pattern(pattern, tag, value):
    result = pattern.match(value.value)
    if result is None:
        raise OBOInvalidFormat(value, tag)
    return result

def get_quoted_value(tag, value):
    return match_pattern(QUOTED_VALUE_PATTERN, tag, value).group('value')

def get_boolean_value(tag, value):
    return match_pattern(BOOLEAN_VALUE_PATTERN, tag, value).group('value') == 'true'

def get_free_value(tag, value):
    return match_pattern(FREE_VALUE_PATTERN, tag, value).group('value').strip()

def get_date_value(tag, value):
    return get_free_value(tag, value)
#    return match_pattern(DATE_VALUE_PATTERN, source, lineno, tag, value).group('date')


class Sourced:
    def __init__(self, source, lineno):
        self.source = source
        self.lineno = lineno

    def message(self, msg=None):
        if msg:
            return '%s:%d: %s' % (self.source, self.lineno, msg)
        return '%s:%d' % (self.source, self.lineno)

    def warning(self, msg):
        stderr.write(self.message(msg + '\n'))

    def duplicate(self, tag):
        return 'duplicate tag ' + tag + ', see: ' + self.message()


class SourcedValue(Sourced):
    def __init__(self, source, lineno, value):
        Sourced.__init__(self, source, lineno)
        self.value = value

        
class SubsetDef(Sourced):
    def __init__(self, source, lineno, name, description):
        Sourced.__init__(self, source, lineno)
        self.name = name
        self.description = description


class SynonymTypeDef(Sourced):
    def __init__(self, source, lineno, name, description, scope):
        Sourced.__init__(self, source, lineno)
        self.name = name
        self.description = description
        self.scope = scope

class HeaderReader(TagReader):
    def __init__(self, ontology, unhandled_tag_option, deprecated_tags_option):
        TagReader.__init__(self, ontology, ontology, unhandled_tag_option, deprecated_tags_option)

    def read_format_version(self, value):
        self.ontology.format_version = get_free_value('format-version', value)

    def read_data_version(self, value):
        self.ontology.version = get_free_value('data-version', value)

    def read_date(self, value):
        self.ontology.date = get_date_value('date', value)

    def read_saved_by(self, value):
        self.ontology.saved_by = get_free_value('saved-by', value)

    def read_auto_generated_by(self, value):
        self.ontology.auto_generated_by = get_free_value('auto-generated-by', value)

    def read_subsetdef(self, value):
        m = match_pattern(SUBSETDEF_PATTERN, 'subsetdef', value)
        name = m.group(1)
        descr = m.group(2)
        if name in self.ontology.subsetdef:
            value.warning(self.ontology.subsetdef[name].duplicate('subsetdef'))
        else:
            self.ontology.subsetdef[name] = SubsetDef(value.source, value.lineno, name, descr)

    def read_synonymtypedef(self, value):
        m = match_pattern(SYNONYMTYPEDEF_PATTERN, 'synonymtypedef', value)
        name = m.group('name')
        descr = m.group('descr')
        scope = m.group('scope')
        if scope is None:
            scope = 'RELATED'
        if name in self.ontology.synonymtypedef:
            value.warning(self.ontology.synonymtypedef[name].duplicate('synonymtypedef'))
        else:
            self.ontology.synonymtypedef[name] = SynonymTypeDef(value.source, value.lineno, name, descr, scope)

    def read_remark(self, value):
        self.ontology.remark.append(get_free_value('remark', value))

    def read_default_namespace(self, value):
        self.ontology.default_namespace = get_free_value('default-namespace', value)


class StanzaReader(Sourced, TagReader):
    def __init__(self, source, lineno, stanza_type, ontology, unhandled_tag_option, deprecated_tags_option):
        Sourced.__init__(self, source, lineno)
        TagReader.__init__(self, None, ontology, unhandled_tag_option, deprecated_tags_option)
        self.stanza_type = stanza_type
        self.stanza = None

    def read(self, tag, value):
        if tag != 'id' and self.stanza is None:
            raise OBOException(value, 'expected tag id')
        TagReader.read(self, tag, value)

    def read_id(self, value):
        if self.stanza is not None:
            raise OBOException(value, self.stanza.duplicate('id'))
        id = get_free_value('id', value)
        if id in self.ontology.builtin:
            raise OBOException(value, 'this id is reserved')
        srcid = SourcedValue(value.source, value.lineno, id)
        if id in self.ontology.stanzas:
            stanza = self.ontology.stanzas[id]
            if not isinstance(stanza, self.stanza_type):
                raise OBOException(value, 'the same id is used for different types of stanzas, see: ' + stanza.message())
            stanza.id = srcid
            self.stanza = stanza
        else:
            self.stanza = self.stanza_type(self.source, self.lineno, self.ontology, srcid)
        self.tagset = self.stanza
        
    def read_name(self, value):
        if self.stanza.name is not None:
            raise OBOException(value, self.stanza.name.duplicate('name'))
        self.stanza.name = SourcedValue(value.source, value.lineno, get_free_value('name', value))

    def read_def(self, value):
        if self.stanza.definition is not None:
            raise OBOException(self.stanza.definition.duplicate('def'))
        m = match_pattern(DEFINITION_PATTERN, 'def', value)
        self.stanza.definition = SourcedValue(value.source, value.lineno, m.group('definition'))

    def read_is_anonymous(self, value):
        self.stanza.is_anonymous = get_boolean_value('is_anonymous', value)

    def read_alt_id(self, value):
        alt_id = get_free_value('alt_id', value)
        self.stanza.alt_ids.append(alt_id)

    def read_comment(self, value):
        if self.stanza.comment is not None:
            raise OBOException(self.stanza.comment.duplicate('comment'))
        self.stanza.comment = SourcedValue(value.source, value.lineno, get_free_value('comment', value))

    def _read_xref(self, tag, value):
        if tag != 'xref':
            self.deprecated_tags_option.handle(self.ontology, tag, value)
        m = match_pattern(XREF_PATTERN, tag, value)
        XRef(value.source, value.lineno, self.stanza, m.group('id'), m.group('descr'))

    def read_xref(self, value):
        self._read_xref('xref', value)

    def read_is_obsolete(self, value):
        self.stanza.is_obsolete = get_boolean_value('is_obsolete', value)

    def read_replaced_by(self, value):
        self._read_simple_ref('replaced_by', value, 'replaced_by')

    def _read_simple_ref(self, tag, value, rel):
        id = get_free_value(tag, value)
        StanzaReference(value.source, value.lineno, self.stanza, rel, id)

    def _read_consider(self, tag, value):
        if tag != 'consider':
            self.deprecated_tags_option.handle(self.ontology, tag, value)
        self._read_simple_ref(tag, value, 'consider')

    def read_consider(self, value):
        self._read_consider('consider', value)

    def read_synonym(self, value):
        m = match_pattern(SYNONYM_PATTERN, 'synonym', value)
        type = m.group('type')
        if type is None:
            default_scope = 'RELATED'
        else:
            if type not in self.ontology.synonymtypedef:
                raise OBOException(value, 'undefined synonym type: ' + type)
            default_scope = self.ontology.synonymtypedef[type].scope
        text = m.group('text')
        scope = m.group('scope')
        if scope is None:
            scope = default_scope
        Synonym(value.source, value.lineno, self.stanza, text, scope, type)


class InstanceReader(StanzaReader):
    def __init__(self, source, lineno, ontology, unhandled_tag_option, deprecated_tags_option):
        StanzaReader.__init__(self, source, lineno, Instance, ontology, unhandled_tag_option, deprecated_tags_option)

    def read_instance_of(self, value):
        ref = get_free_value('instance_of', value)
        StanzaReference(value.source, value.lineno, self.stanza, 'instance_of', ref)

    def read_property_value(self, value):
        m = match_pattern(INSTANCE_PROPERTY_VALUE_PATTERN, 'property_value', value)
        ref = StanzaReference(value.source, value.lineno, self.stanza, m.group('rel'), m.group('ref'))
        if m.group('value'):
            ref.value = m.group('value')


class TermOrTypeReader(StanzaReader):
    def __init__(self, source, lineno, stanza_type, ontology, unhandled_tag_option, deprecated_tags_option):
        StanzaReader.__init__(self, source, lineno, stanza_type, ontology, unhandled_tag_option, deprecated_tags_option)

    def read_subset(self, value):
        subset = get_free_value('subset', value)
        if subset not in self.ontology.subsetdef:
            raise OBOException(value, 'undefined subset ' + subset + ' (' + str(self.ontology.subsetdef) + ')')
        if subset in self.stanza.subsets:
            value.warning(self.ontology.subsetdef[subset].duplicate('subsetdef'))
        self.stanza.subsets.add(subset)

    def _read_deprecated_synonym(self, tag, value, scope):
        self.deprecated_tags_option.handle(self.ontology, tag, value)
        m = match_pattern(DEPRECATED_SYNONYM_PATTERN, tag, value)
        type = m.group('type')
        if type is not None:
            if type not in self.ontology.synonymtypedef:
                raise OBOException(value, 'undefined synonym type: ' + type)
            default_scope = self.ontology.synonymtypedef[type].scope
        text = m.group('text')
        Synonym(value.source, value.lineno, self.stanza, text, scope, type)

    def read_exact_synonym(self, value):
        self._read_deprecated_synonym('exact_synonym', value, 'EXACT')

    def read_narrow_synonym(self, value):
        self._read_deprecated_synonym('narrow_synonym', value, 'NARROW')

    def read_related_synonym(self, value):
        self._read_deprecated_synonym('related_synonym', value, 'RELATED')

    def read_broad_synonym(self, value):
        self._read_deprecated_synonym('broad_synonym', value, 'BROAD')

    def read_xref_analog(self, value):
        self._read_xref('xref_analog', value)

    def read_xref_unk(self, value):
        self._read_xref('xref_unk', value)

    def read_is_a(self, value):
        self._read_simple_ref('is_a', value, 'is_a')

    def read_relationship(self, value):
        m = match_pattern(RELATIONSHIP_PATTERN, 'relationship', value)
        StanzaReference(value.source, value.lineno, self.stanza, m.group('rel'), m.group('id'))

    def read_use_term(self, value):
        self._read_consider('use_term', value)

    def read_created_by(self, value):
        self.stanza.created_by = get_free_value('created_by', value)

    def read_creation_date(self, value):
        self.stanza.creation_date = get_date_value('creation_date', value)



class TermReader(TermOrTypeReader):
    def __init__(self, source, lineno, ontology, unhandled_tag_option, deprecated_tags_option):
        TermOrTypeReader.__init__(self, source, lineno, Term, ontology, unhandled_tag_option, deprecated_tags_option)

    def read_union_of(self, value):
        self._read_simple_ref('union_of', value, 'union_of')

    def read_disjoint_from(self, value):
        self._read_simple_ref('disjoint_from', value, 'disjoint_from')

    def read_intersection_of(self, value):
        m = match_pattern(INTERSECTION_PATTERN, 'intersection_of', value)
        rel = m.group('rel')
        if rel is None:
            rel = 'is_a'
        StanzaReference(value.source, value.lineno, self.stanza, rel, m.group('id'), collection_attribute='intersection_of')
        


class TypedefReader(TermOrTypeReader):
    def __init__(self, source, lineno, ontology, unhandled_tag_option, deprecated_tags_option):
        TermOrTypeReader.__init__(self, source, lineno, Typedef, ontology, unhandled_tag_option, deprecated_tags_option)

    def read_domain(self, value):
        StanzaReference(value.source, value.lineno, self.stanza, 'domain', get_free_value('domain', value))

    def read_range(self, value):
        StanzaReference(value.source, value.lineno, self.stanza, 'range', get_free_value('range', value))

    def read_inverse_of(self, value):
        StanzaReference(value.source, value.lineno, self.stanza, 'inverse_of', get_free_value('inverse_of', value))

    def read_transitive_over(self, value):
        StanzaReference(value.source, value.lineno, self.stanza, 'transitive_over', get_free_value('transitive_over', value))

    def read_is_cyclic(self, value):
        self.stanza.is_cyclic = get_boolean_value('is_cyclic', value)

    def read_is_reflexive(self, value):
        self.stanza.is_reflexive = get_boolean_value('is_reflexive', value)

    def read_is_symmetric(self, value):
        self.stanza.is_symmetric = get_boolean_value('is_symmetric', value)

    def read_is_anti_symmetric(self, value):
        self.stanza.is_anti_symmetric = get_boolean_value('is_anti_symmetric', value)

    def read_is_transitive(self, value):
        self.stanza.is_transitive = get_boolean_value('is_transitive', value)

    def read_is_metadata_tag(self, value):
        self.stanza.is_metadata_tag = get_boolean_value('is_metadata_tag', value)

    


class StanzaReference(Sourced):
    def __init__(self, source, lineno, stanza, rel, reference, collection_attribute='references'):
        Sourced.__init__(self, source, lineno)
        self.stanza = stanza
        self.rel = rel
        self.reference = reference
        c = getattr(stanza, collection_attribute)
        if rel in c:
            l = c[rel]
        else:
            l = []
            c[rel] = l
        l.append(self)

    def resolve_reference(self, rel_object, dangling_reference_option):
        if rel_object is not None:
            self.rel_object = rel_object
        if self.reference not in self.stanza.ontology.stanzas:
            dangling_reference_option.handle(self, self.reference)
            return
        self.reference_object = self.stanza.ontology.stanzas[self.reference]
        # XXX check range


class XRef(Sourced):
    def __init__(self, source, lineno, term, reference, description):
        Sourced.__init__(self, source, lineno)
        self.term = term
        self.reference = reference
        self.description = description


class Synonym(Sourced):
    def __init__(self, source, lineno, stanza, text, scope, type):
        Sourced.__init__(self, source, lineno)
        self.stanza = stanza
        self.text = text
        self.scope = scope
        self.type = type
        stanza.synonyms.append(self)

    def resolve_references(self, dangling_reference_option):
        if self.type is None:
            return
        if self.type not in self.stanza.ontology.synonymtypedef:
            dangling_reference_option.handle(self, self.type)
            return
        self.type_object = self.stanza.ontology.synonymtypedef[self.type]


class TagSet:
    def __init__(self):
        self.unhandled_tags = []


class BuiltinStanza:
    def __init__(self, ontology, id):
        self.ontology = ontology
        self.id = id
        self.ontology.stanzas[id] = self
        self.ontology.builtin[id] = self

    def resolve_references(self, dangling_reference_option):
        pass

    def check_required(self):
        pass
    

class BuiltinTermOrType(BuiltinStanza):
    def __init__(self, ontology):
        BuiltinStanza.__init__(self, ontology, 'OBO:TERM_OR_TYPE')

class BuiltinTerm(BuiltinStanza):
    def __init__(self, ontology):
        BuiltinStanza.__init__(self, ontology, 'OBO:TERM')

class BuiltinType(BuiltinStanza):
    def __init__(self, ontology):
        BuiltinStanza.__init__(self, ontology, 'OBO:TYPE')

class BuiltinInstance(BuiltinStanza):
    def __init__(self, ontology):
        BuiltinStanza.__init__(self, ontology, 'OBO:INSTANCE')



class Stanza(Sourced, TagSet):
    def __init__(self, source, lineno, ontology, id):
        Sourced.__init__(self, source, lineno)
        TagSet.__init__(self)
        self.ontology = ontology
        self.id = id
        ontology.stanzas[id.value] = self
        self.name = None
        self.is_anonymous = False
        self.alt_ids = []
        self.comment = None
        self.synonyms = []
        self.references = {}
        self.is_obsolete = False
        self.created_by = None
        self.creation_date = None
        self.definition = None

    def check_required(self):
        if self.name is None:
            raise OBOException(self, 'missing required tag name')

    def _resolve_relation_references(self, c, dangling_reference_option):
        for rt, refs in c.iteritems():
            if rt not in self.ontology.stanzas:
                dangling_reference_option.handle(self, rt)
                rt_object = None
            else:
                rt_object = self.ontology.stanzas[rt]
                if not isinstance(rt_object, Typedef):
                    raise OBOException('this is not a relation type: ' + rt + '\n    ' + '\n    '.join(ref.message() for ref in refs))
                # XXX check domain
                for ref in refs:
                    ref.resolve_reference(rt_object, dangling_reference_option)

    def resolve_references(self, dangling_reference_option):
        for syn in self.synonyms:
            syn.resolve_references(dangling_reference_option)
        self._resolve_relation_references(self.references, dangling_reference_option)

    def ancestors(self, rel='is_a', include_self=False):
        if include_self:
            yield self
        if rel in self.references:
            for link in self.references[rel]:
                for a in link.reference_object.ancestors(rel, include_self=True):
                    yield a

    def paths(self, rel='is_a', include_self=False):
        if rel in self.references:
            for link in self.references[rel]:
                for parent_path in link.reference_object.paths(rel, include_self=True):
                    if include_self:
                        parent_path.append(self)
                    yield parent_path
        elif include_self:
            yield [self]
        else:
            yield []


class TermOrType(Stanza):
    def __init__(self, source, lineno, ontology, id):
        Stanza.__init__(self, source, lineno, ontology, id)
        self.subsets = set()

    def resolve_references(self, dangling_reference_option):
        Stanza.resolve_references(self, dangling_reference_option)
        self.subset_objects = []
        for s in self.subsets:
            if s not in self.ontology.subsetdef:
                dangling_reference_option.handle(self, s)
            else:
                self.subset_objects.append(self.ontology.subsetdef[s])
                


class Term(TermOrType):
    def __init__(self, source, lineno, ontology, id):
        TermOrType.__init__(self, source, lineno, ontology, id)
        self.intersection_of = {}

    def resolve_references(self, dangling_reference_option):
        TermOrType.resolve_references(self, dangling_reference_option)
        self._resolve_relation_references(self.intersection_of, dangling_reference_option)


class Typedef(TermOrType):
    def __init__(self, source, lineno, ontology, id):
        TermOrType.__init__(self, source, lineno, ontology, id)


class Instance(Stanza):
    def __init__(self, source, lineno, ontology, id):
        Stanza.__init__(self, source, lineno, ontology, id)

    def check_required(self):
        if 'instance_of' not in self.references:
            raise OBOException(self, 'missing required tag instance_of')


class DanglingReferenceOption:
    def __init__(self):
        pass

    def handle(self, sourced, ref):
        raise NotImplemented()


class DanglingReferenceFail(DanglingReferenceOption):
    def __init__(self):
        DanglingReferenceOption.__init__(self)

    def handle(self, sourced, ref):
        raise OBOException(sourced, 'unknown reference to ' + str(ref))

class DanglingReferenceRead(DanglingReferenceOption):
    def __init__(self):
        DanglingReferenceOption.__init__(self)

    def handle(self, sourced, ref):
        pass

class DanglingReferenceIgnore(DanglingReferenceOption):
    def __init__(self):
        DanglingReferenceOption.__init__(self)

    def handle(self, sourced, ref):
        pass

class DanglingReferenceWarn(DanglingReferenceOption):
    def __init__(self):
        DanglingReferenceOption.__init__(self)

    def handle(self, sourced, ref):
        sourced.warning('unknown reference to ' + ref)

class DanglingReferenceWarnAndRead(DanglingReferenceWarn, DanglingReferenceRead):
    def __init__(self):
        DanglingReferenceWarn.__init__(self)
        DanglingReferenceRead.__init__(self)

    def handle(self, sourced, ref):
        DanglingReferenceWarn.handle(self, sourced, ref)
        DanglingReferenceRead.handle(self, sourced, ref)

class DanglingReferenceWarnAndIgnore(DanglingReferenceWarn, DanglingReferenceIgnore):
    def __init__(self):
        DanglingReferenceWarn.__init__(self)
        DanglingReferenceIgnore.__init__(self)

    def handle(self, sourced, ref):
        DanglingReferenceWarn.handle(self, sourced, ref)
        DanglingReferenceIgnore.handle(self, sourced, ref)



STANZA_TYPE_PATTERN = re.compile('\[(?P<stanza_type>\S+)\]' + TERMINAL_COMMENT)
TAG_VALUE_PATTERN = re.compile('(?P<tag>(?:[^:]|\\.)+):(?P<value>.*)')

class OntologyReader:
    def __init__(self, ontology):
        self.ontology = ontology
        self.header_reader = HeaderReader(ontology, None, None)
        self.stanza_readers = {
            'Term': TermReader(None, 0, ontology, None, None),
            'Typedef': TypedefReader(None, 0, ontology, None, None),
            'Instance': InstanceReader(None, 0, ontology, None, None)
            }

    def read(self, source, file, unhandled_tag_option, deprecated_tags_option):
        current_reader = self.header_reader
        current_reader.unhandled_tag_option = unhandled_tag_option
        current_reader.deprecated_tags_option = deprecated_tags_option
        for r in self.stanza_readers.itervalues():
            r.unhandled_tag_option = unhandled_tag_option
            r.deprecated_tags_option = deprecated_tags_option
        lineno = 0
        for line in file:
            lineno += 1
            line = line.strip()
            if line == '':
                continue
            if line[0] == '!':
                continue
            m = STANZA_TYPE_PATTERN.match(line)
            if m is not None:
                stanza_type_name = m.group('stanza_type')
                if stanza_type_name not in self.stanza_readers:
                    raise OBOException(Sourced(source, lineno), 'unhandled stanza type ' + stanza_type_name)
                current_reader = self.stanza_readers[stanza_type_name]
                current_reader.source = source
                current_reader.lineno = lineno
                current_reader.stanza = None
                continue
            m = TAG_VALUE_PATTERN.match(line)
            if m is None:
                raise OBOException(Sourced(source, lineno), 'syntax error')
            tag = m.group('tag').strip()
            value = m.group('value').strip()
            current_reader.read(tag, SourcedValue(source, lineno, value))



        

BUILTIN = '''[Typedef]
id: is_a
name: is_a
range: OBO:TERM_OR_TYPE
domain: OBO:TERM_OR_TYPE
def: "The basic subclassing relationship" [OBO:defs]

[Typedef]
id: disjoint_from
name: disjoint_from
range: OBO:TERM
domain: OBO:TERM
def: "Indicates that two classes are disjoint" [OBO:defs]

[Typedef]
id: instance_of
name: instance_of
range: OBO:TERM
domain: OBO:INSTANCE
def: "Indicates the type of an instance" [OBO:defs]

[Typedef]
id: inverse_of
name: inverse_of
range: OBO:TYPE
domain: OBO:TYPE
def: "Indicates that one relationship type is the inverse of another" [OBO:defs]

[Typedef]
id: union_of
name: union_of
range: OBO:TERM
domain: OBO:TERM
def: "Indicates that a term is the union of several others" [OBO:defs]

[Typedef]
id: intersection_of
name: intersection_of
range: OBO:TERM
domain: OBO:TERM
def: "Indicates that a term is the intersection of several others" [OBO:defs]

[Typedef]
id: range
name: range
range: OBO:TERM_OR_TYPE
domain: OBO:TYPE
def: "Indicates the range (type of target) of a relation"

[Typedef]
id: domain
name: domain
range: OBO:TERM_OR_TYPE
domain: OBO:TYPE
def: "Indicates the domain (type of source) of a relation"
'''





class Ontology(TagSet):
    def __init__(self):
        TagSet.__init__(self)
        self.synonymtypedef = {}
        self.remark = []
        self.stanzas = {}
        self.builtin = {}
        self.subsetdef = {}
        self.format_version = '1.2'
        self.version = None
        self.date = None
        self.saved_by = None
        self.auto_generated_by = None
        self.default_namesapce = None
        OntologyReader(self).read('<<builtin>>', StringIO(BUILTIN), UnhandledTagFail(), DeprecatedTagWarn())
        BuiltinType(self)
        BuiltinInstance(self)
        BuiltinTerm(self)
        BuiltinTermOrType(self)

    def load_files(self, unhandled_tag_option, *filenames):
        reader = OntologyReader(self)
        for fn in filenames:
            f = open(fn)
            reader.read(fn, f, unhandled_tag_option)
            f.close()

    def load_stdin(self, unhandled_tag_option):
        reader = OntologyReader(self)
        reader.read('<<stdin>>', stdin, unhandled_tag_option)

    def resolve_references(self, dangling_reference_option):
        for s in self.stanzas.itervalues():
            s.resolve_references(dangling_reference_option)

    def check_required(self):
        for s in self.stanzas.itervalues():
            s.check_required()

    def iterterms(self):
        for term in self.stanzas.itervalues():
            if isinstance(term, Term):
                yield term


if __name__ == '__main__':
    onto = Ontology()
    onto.load_stdin(UnhandledTagFail())
    onto.check_required()
    onto.resolve_references(DanglingReferenceFail())
