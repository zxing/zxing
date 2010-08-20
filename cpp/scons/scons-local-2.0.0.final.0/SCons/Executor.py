"""SCons.Executor

A module for executing actions with specific lists of target and source
Nodes.

"""

#
# Copyright (c) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010 The SCons Foundation
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
#
# The above copyright notice and this permission notice shall be included
# in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
# KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
# WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

__revision__ = "src/engine/SCons/Executor.py 5023 2010/06/14 22:05:46 scons"

import collections

from SCons.Debug import logInstanceCreation
import SCons.Errors
import SCons.Memoize


class Batch(object):
    """Remembers exact association between targets
    and sources of executor."""
    def __init__(self, targets=[], sources=[]):
        self.targets = targets
        self.sources = sources



class TSList(collections.UserList):
    """A class that implements $TARGETS or $SOURCES expansions by wrapping
    an executor Method.  This class is used in the Executor.lvars()
    to delay creation of NodeList objects until they're needed.

    Note that we subclass collections.UserList purely so that the
    is_Sequence() function will identify an object of this class as
    a list during variable expansion.  We're not really using any
    collections.UserList methods in practice.
    """
    def __init__(self, func):
        self.func = func
    def __getattr__(self, attr):
        nl = self.func()
        return getattr(nl, attr)
    def __getitem__(self, i):
        nl = self.func()
        return nl[i]
    def __getslice__(self, i, j):
        nl = self.func()
        i = max(i, 0); j = max(j, 0)
        return nl[i:j]
    def __str__(self):
        nl = self.func()
        return str(nl)
    def __repr__(self):
        nl = self.func()
        return repr(nl)

class TSObject(object):
    """A class that implements $TARGET or $SOURCE expansions by wrapping
    an Executor method.
    """
    def __init__(self, func):
        self.func = func
    def __getattr__(self, attr):
        n = self.func()
        return getattr(n, attr)
    def __str__(self):
        n = self.func()
        if n:
            return str(n)
        return ''
    def __repr__(self):
        n = self.func()
        if n:
            return repr(n)
        return ''

def rfile(node):
    """
    A function to return the results of a Node's rfile() method,
    if it exists, and the Node itself otherwise (if it's a Value
    Node, e.g.).
    """
    try:
        rfile = node.rfile
    except AttributeError:
        return node
    else:
        return rfile()


class Executor(object):
    """A class for controlling instances of executing an action.

    This largely exists to hold a single association of an action,
    environment, list of environment override dictionaries, targets
    and sources for later processing as needed.
    """

    if SCons.Memoize.use_memoizer:
        __metaclass__ = SCons.Memoize.Memoized_Metaclass

    memoizer_counters = []

    def __init__(self, action, env=None, overridelist=[{}],
                 targets=[], sources=[], builder_kw={}):
        if __debug__: logInstanceCreation(self, 'Executor.Executor')
        self.set_action_list(action)
        self.pre_actions = []
        self.post_actions = []
        self.env = env
        self.overridelist = overridelist
        if targets or sources:
            self.batches = [Batch(targets[:], sources[:])]
        else:
            self.batches = []
        self.builder_kw = builder_kw
        self._memo = {}

    def get_lvars(self):
        try:
            return self.lvars
        except AttributeError:
            self.lvars = {
                'CHANGED_SOURCES' : TSList(self._get_changed_sources),
                'CHANGED_TARGETS' : TSList(self._get_changed_targets),
                'SOURCE' : TSObject(self._get_source),
                'SOURCES' : TSList(self._get_sources),
                'TARGET' : TSObject(self._get_target),
                'TARGETS' : TSList(self._get_targets),
                'UNCHANGED_SOURCES' : TSList(self._get_unchanged_sources),
                'UNCHANGED_TARGETS' : TSList(self._get_unchanged_targets),
            }
            return self.lvars

    def _get_changes(self):
        cs = []
        ct = []
        us = []
        ut = []
        for b in self.batches:
            if b.targets[0].is_up_to_date():
                us.extend(list(map(rfile, b.sources)))
                ut.extend(b.targets)
            else:
                cs.extend(list(map(rfile, b.sources)))
                ct.extend(b.targets)
        self._changed_sources_list = SCons.Util.NodeList(cs)
        self._changed_targets_list = SCons.Util.NodeList(ct)
        self._unchanged_sources_list = SCons.Util.NodeList(us)
        self._unchanged_targets_list = SCons.Util.NodeList(ut)

    def _get_changed_sources(self, *args, **kw):
        try:
            return self._changed_sources_list
        except AttributeError:
            self._get_changes()
            return self._changed_sources_list

    def _get_changed_targets(self, *args, **kw):
        try:
            return self._changed_targets_list
        except AttributeError:
            self._get_changes()
            return self._changed_targets_list

    def _get_source(self, *args, **kw):
        #return SCons.Util.NodeList([rfile(self.batches[0].sources[0]).get_subst_proxy()])
        return rfile(self.batches[0].sources[0]).get_subst_proxy()

    def _get_sources(self, *args, **kw):
        return SCons.Util.NodeList([rfile(n).get_subst_proxy() for n in self.get_all_sources()])

    def _get_target(self, *args, **kw):
        #return SCons.Util.NodeList([self.batches[0].targets[0].get_subst_proxy()])
        return self.batches[0].targets[0].get_subst_proxy()

    def _get_targets(self, *args, **kw):
        return SCons.Util.NodeList([n.get_subst_proxy() for n in self.get_all_targets()])

    def _get_unchanged_sources(self, *args, **kw):
        try:
            return self._unchanged_sources_list
        except AttributeError:
            self._get_changes()
            return self._unchanged_sources_list

    def _get_unchanged_targets(self, *args, **kw):
        try:
            return self._unchanged_targets_list
        except AttributeError:
            self._get_changes()
            return self._unchanged_targets_list

    def get_action_targets(self):
        if not self.action_list:
            return []
        targets_string = self.action_list[0].get_targets(self.env, self)
        if targets_string[0] == '$':
            targets_string = targets_string[1:]
        return self.get_lvars()[targets_string]

    def set_action_list(self, action):
        import SCons.Util
        if not SCons.Util.is_List(action):
            if not action:
                import SCons.Errors
                raise SCons.Errors.UserError("Executor must have an action.")
            action = [action]
        self.action_list = action

    def get_action_list(self):
        return self.pre_actions + self.action_list + self.post_actions

    def get_all_targets(self):
        """Returns all targets for all batches of this Executor."""
        result = []
        for batch in self.batches:
            result.extend(batch.targets)
        return result

    def get_all_sources(self):
        """Returns all sources for all batches of this Executor."""
        result = []
        for batch in self.batches:
            result.extend(batch.sources)
        return result

    def get_all_children(self):
        """Returns all unique children (dependencies) for all batches
        of this Executor.

        The Taskmaster can recognize when it's already evaluated a
        Node, so we don't have to make this list unique for its intended
        canonical use case, but we expect there to be a lot of redundancy
        (long lists of batched .cc files #including the same .h files
        over and over), so removing the duplicates once up front should
        save the Taskmaster a lot of work.
        """
        result = SCons.Util.UniqueList([])
        for target in self.get_all_targets():
            result.extend(target.children())
        return result

    def get_all_prerequisites(self):
        """Returns all unique (order-only) prerequisites for all batches
        of this Executor.
        """
        result = SCons.Util.UniqueList([])
        for target in self.get_all_targets():
            result.extend(target.prerequisites)
        return result

    def get_action_side_effects(self):

        """Returns all side effects for all batches of this
        Executor used by the underlying Action.
        """
        result = SCons.Util.UniqueList([])
        for target in self.get_action_targets():
            result.extend(target.side_effects)
        return result

    memoizer_counters.append(SCons.Memoize.CountValue('get_build_env'))

    def get_build_env(self):
        """Fetch or create the appropriate build Environment
        for this Executor.
        """
        try:
            return self._memo['get_build_env']
        except KeyError:
            pass

        # Create the build environment instance with appropriate
        # overrides.  These get evaluated against the current
        # environment's construction variables so that users can
        # add to existing values by referencing the variable in
        # the expansion.
        overrides = {}
        for odict in self.overridelist:
            overrides.update(odict)

        import SCons.Defaults
        env = self.env or SCons.Defaults.DefaultEnvironment()
        build_env = env.Override(overrides)

        self._memo['get_build_env'] = build_env

        return build_env

    def get_build_scanner_path(self, scanner):
        """Fetch the scanner path for this executor's targets and sources.
        """
        env = self.get_build_env()
        try:
            cwd = self.batches[0].targets[0].cwd
        except (IndexError, AttributeError):
            cwd = None
        return scanner.path(env, cwd,
                            self.get_all_targets(),
                            self.get_all_sources())

    def get_kw(self, kw={}):
        result = self.builder_kw.copy()
        result.update(kw)
        result['executor'] = self
        return result

    def do_nothing(self, target, kw):
        return 0

    def do_execute(self, target, kw):
        """Actually execute the action list."""
        env = self.get_build_env()
        kw = self.get_kw(kw)
        status = 0
        for act in self.get_action_list():
            #args = (self.get_all_targets(), self.get_all_sources(), env)
            args = ([], [], env)
            status = act(*args, **kw)
            if isinstance(status, SCons.Errors.BuildError):
                status.executor = self
                raise status
            elif status:
                msg = "Error %s" % status
                raise SCons.Errors.BuildError(
                    errstr=msg, 
                    node=self.batches[0].targets,
                    executor=self, 
                    action=act)
        return status

    # use extra indirection because with new-style objects (Python 2.2
    # and above) we can't override special methods, and nullify() needs
    # to be able to do this.

    def __call__(self, target, **kw):
        return self.do_execute(target, kw)

    def cleanup(self):
        self._memo = {}

    def add_sources(self, sources):
        """Add source files to this Executor's list.  This is necessary
        for "multi" Builders that can be called repeatedly to build up
        a source file list for a given target."""
        # TODO(batch):  extend to multiple batches
        assert (len(self.batches) == 1)
        # TODO(batch):  remove duplicates?
        sources = [x for x in sources if x not in self.batches[0].sources]
        self.batches[0].sources.extend(sources)

    def get_sources(self):
        return self.batches[0].sources

    def add_batch(self, targets, sources):
        """Add pair of associated target and source to this Executor's list.
        This is necessary for "batch" Builders that can be called repeatedly
        to build up a list of matching target and source files that will be
        used in order to update multiple target files at once from multiple
        corresponding source files, for tools like MSVC that support it."""
        self.batches.append(Batch(targets, sources))

    def prepare(self):
        """
        Preparatory checks for whether this Executor can go ahead
        and (try to) build its targets.
        """
        for s in self.get_all_sources():
            if s.missing():
                msg = "Source `%s' not found, needed by target `%s'."
                raise SCons.Errors.StopError(msg % (s, self.batches[0].targets[0]))

    def add_pre_action(self, action):
        self.pre_actions.append(action)

    def add_post_action(self, action):
        self.post_actions.append(action)

    # another extra indirection for new-style objects and nullify...

    def my_str(self):
        env = self.get_build_env()
        return "\n".join([action.genstring(self.get_all_targets(),
                                           self.get_all_sources(),
                                           env)
                          for action in self.get_action_list()])


    def __str__(self):
        return self.my_str()

    def nullify(self):
        self.cleanup()
        self.do_execute = self.do_nothing
        self.my_str     = lambda: ''

    memoizer_counters.append(SCons.Memoize.CountValue('get_contents'))

    def get_contents(self):
        """Fetch the signature contents.  This is the main reason this
        class exists, so we can compute this once and cache it regardless
        of how many target or source Nodes there are.
        """
        try:
            return self._memo['get_contents']
        except KeyError:
            pass
        env = self.get_build_env()
        result = "".join([action.get_contents(self.get_all_targets(),
                                              self.get_all_sources(),
                                              env)
                          for action in self.get_action_list()])
        self._memo['get_contents'] = result
        return result

    def get_timestamp(self):
        """Fetch a time stamp for this Executor.  We don't have one, of
        course (only files do), but this is the interface used by the
        timestamp module.
        """
        return 0

    def scan_targets(self, scanner):
        # TODO(batch):  scan by batches
        self.scan(scanner, self.get_all_targets())

    def scan_sources(self, scanner):
        # TODO(batch):  scan by batches
        if self.batches[0].sources:
            self.scan(scanner, self.get_all_sources())

    def scan(self, scanner, node_list):
        """Scan a list of this Executor's files (targets or sources) for
        implicit dependencies and update all of the targets with them.
        This essentially short-circuits an N*M scan of the sources for
        each individual target, which is a hell of a lot more efficient.
        """
        env = self.get_build_env()

        # TODO(batch):  scan by batches)
        deps = []
        if scanner:
            for node in node_list:
                node.disambiguate()
                s = scanner.select(node)
                if not s:
                    continue
                path = self.get_build_scanner_path(s)
                deps.extend(node.get_implicit_deps(env, s, path))
        else:
            kw = self.get_kw()
            for node in node_list:
                node.disambiguate()
                scanner = node.get_env_scanner(env, kw)
                if not scanner:
                    continue
                scanner = scanner.select(node)
                if not scanner:
                    continue
                path = self.get_build_scanner_path(scanner)
                deps.extend(node.get_implicit_deps(env, scanner, path))

        deps.extend(self.get_implicit_deps())

        for tgt in self.get_all_targets():
            tgt.add_to_implicit(deps)

    def _get_unignored_sources_key(self, node, ignore=()):
        return (node,) + tuple(ignore)

    memoizer_counters.append(SCons.Memoize.CountDict('get_unignored_sources', _get_unignored_sources_key))

    def get_unignored_sources(self, node, ignore=()):
        key = (node,) + tuple(ignore)
        try:
            memo_dict = self._memo['get_unignored_sources']
        except KeyError:
            memo_dict = {}
            self._memo['get_unignored_sources'] = memo_dict
        else:
            try:
                return memo_dict[key]
            except KeyError:
                pass

        if node:
            # TODO:  better way to do this (it's a linear search,
            # but it may not be critical path)?
            sourcelist = []
            for b in self.batches:
                if node in b.targets:
                    sourcelist = b.sources
                    break
        else:
            sourcelist = self.get_all_sources()
        if ignore:
            idict = {}
            for i in ignore:
                idict[i] = 1
            sourcelist = [s for s in sourcelist if s not in idict]

        memo_dict[key] = sourcelist

        return sourcelist

    def get_implicit_deps(self):
        """Return the executor's implicit dependencies, i.e. the nodes of
        the commands to be executed."""
        result = []
        build_env = self.get_build_env()
        for act in self.get_action_list():
            deps = act.get_implicit_deps(self.get_all_targets(),
                                         self.get_all_sources(),
                                         build_env)
            result.extend(deps)
        return result



_batch_executors = {}

def GetBatchExecutor(key):
    return _batch_executors[key]

def AddBatchExecutor(key, executor):
    assert key not in _batch_executors
    _batch_executors[key] = executor

nullenv = None


def get_NullEnvironment():
    """Use singleton pattern for Null Environments."""
    global nullenv

    import SCons.Util
    class NullEnvironment(SCons.Util.Null):
        import SCons.CacheDir
        _CacheDir_path = None
        _CacheDir = SCons.CacheDir.CacheDir(None)
        def get_CacheDir(self):
            return self._CacheDir

    if not nullenv:
        nullenv = NullEnvironment()
    return nullenv

class Null(object):
    """A null Executor, with a null build Environment, that does
    nothing when the rest of the methods call it.

    This might be able to disapper when we refactor things to
    disassociate Builders from Nodes entirely, so we're not
    going to worry about unit tests for this--at least for now.
    """
    def __init__(self, *args, **kw):
        if __debug__: logInstanceCreation(self, 'Executor.Null')
        self.batches = [Batch(kw['targets'][:], [])]
    def get_build_env(self):
        return get_NullEnvironment()
    def get_build_scanner_path(self):
        return None
    def cleanup(self):
        pass
    def prepare(self):
        pass
    def get_unignored_sources(self, *args, **kw):
        return tuple(())
    def get_action_targets(self):
        return []
    def get_action_list(self):
        return []
    def get_all_targets(self):
        return self.batches[0].targets
    def get_all_sources(self):
        return self.batches[0].targets[0].sources
    def get_all_children(self):
        return self.get_all_sources()
    def get_all_prerequisites(self):
        return []
    def get_action_side_effects(self):
        return []
    def __call__(self, *args, **kw):
        return 0
    def get_contents(self):
        return ''
    def _morph(self):
        """Morph this Null executor to a real Executor object."""
        batches = self.batches
        self.__class__ = Executor
        self.__init__([])            
        self.batches = batches

    # The following methods require morphing this Null Executor to a
    # real Executor object.

    def add_pre_action(self, action):
        self._morph()
        self.add_pre_action(action)
    def add_post_action(self, action):
        self._morph()
        self.add_post_action(action)
    def set_action_list(self, action):
        self._morph()
        self.set_action_list(action)


# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
