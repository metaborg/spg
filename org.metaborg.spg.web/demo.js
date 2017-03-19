/**
 * Domain
 */
var None = new None();

// Options
function None() {
}

function Some(x) {
	return x;
}

function Parent() {
	return null;
}

function Program(pattern, constraints, typeEnv, resolution, subtypes) {
	if (!(this instanceof Program)) {
		return new Program(pattern, constraints, typeEnv, resolution, subtypes);
	}

	this.pattern = pattern;
	this.constraints = constraints;
	this.typeEnv = typeEnv;
	this.resolution = resolution;
	this.subtypes = subtypes;
}

Program.prototype.holes = function () {
	return this.constraints.reduce(function (acc, e) {
		if (e instanceof CGenRecurse) {
			return acc + 1;
		} else {
			return acc;
		}
	}, 0);
};

Program.prototype.size = function () {
	return this.pattern.size();
};

// Type Environment
function TypeEnv(map) {
	if (!(this instanceof TypeEnv)) {
		return new TypeEnv(map);
	}

	this.map = map;
}

function Binding(name, type) {
	if (!(this instanceof Binding)) {
		return new Binding(name, type);
	}

	this.name = name;
	this.type = type;
}

Binding.prototype.toString = function () {
	return this.name + ": " + this.type;
};

// Resolution
function Resolution(map) {
	if (!(this instanceof Resolution)) {
		return new Resolution(map);
	}

	this.map = map;
}

function Subtypes(bindings) {
	if (!(this instanceof Subtypes)) {
		return new Subtypes(bindings);
	}

	this.bindings = bindings;
}

// Term
function TermAppl(name, children) {
	if (!(this instanceof TermAppl)) {
		return new TermAppl(name, children);
	}

	this.name = name;
	this.children = children || [];
}

TermAppl.prototype.toString = function () {
	var stringTypes = this.children.map(function (type) {
		return type.toString();
	});

	if (this.children.length == 0) {
		return mathMode(this.name);
	} else {
		return mathMode(this.name) + "(" + stringTypes.join(", ") + ")";
	}
};

TermAppl.prototype.size = function () {
	var sizes = this.children.map(function (child) {
		return child.size();
	});

	var sum = sizes.reduce(function (acc, e) {
		return acc + e;
	}, 0);

	return 1 + sum;
};

function Var(name, sort, types, scopes) {
	if (!(this instanceof Var)) {
		return new Var(name, sort, types, scopes);
	}

	this.name = name;
	this.sort = sort;
	this.types = types;
	this.scopes = scopes;
}

Var.prototype.size = function () {
	return 1;
};

Var.prototype.toString = function () {
	return mathMode(this.name);
};

function As(alias, term) {
	if (!(this instanceof As)) {
		return new As(alias, term);
	}

	this.alias = alias;
	this.term = term;
	this.children = term.children || [];
}

As.prototype.size = function () {
	return this.term.size();
};

As.prototype.toString = function () {
	return this.alias + "@" + this.term;
};

function TermListCons(head, tail) {
	if (!(this instanceof TermListCons)) {
		return new TermListCons(head, tail);
	}

	this.head = head;
	this.tail = tail;
}

TermListCons.prototype.toString = function () {
	return "[" + this.head + "|" + this.tail + "]";
};

TermListCons.prototype.size = function () {
	return 1 + this.head.size() + this.tail.size();
};

function TermListNil() {
	if (!(this instanceof TermListNil)) {
		return new TermListNil();
	}
}

TermListNil.prototype.toString = function () {
	return "[]";
};

TermListNil.prototype.size = function () {
	return 1;
};

function TermString(name) {
	if (!(this instanceof TermString)) {
		return new TermString(name);
	}

	this.name = name;
}

TermString.prototype.size = function () {
	return 1;
};

TermString.prototype.toString = function () {
	return this.name;
};



// Sort
function SortAppl(name, children) {
	if (!(this instanceof SortAppl)) {
		return new SortAppl(name, children);
	}

	this.name = name;
	this.children = children;
}

SortAppl.prototype.toString = function () {
	var children = this.children.map(function (sort) {
		return sort.toString();
	});

	if (children.length > 0) {
		return this.name + "(" + children.join(", ") + ")";
	} else {
		return this.name;
	}
};

function SortVar(name) {
	if (!(this instanceof SortVar)) {
		return new SortVar(name);
	}

	this.name = name;
}

SortVar.prototype.toString = function () {
	return this.name;
};

// Type
function TypeAppl(name, children) {
	if (!(this instanceof TypeAppl)) {
		return new TypeAppl(name, children);
	}

	this.name = name;
	this.children = children;
}

TypeAppl.prototype.toString = function () {
	var stringTypes = this.children.map(function (type) {
		return type.toString();
	});

	return this.name + "(" + stringTypes.join(", ") + ")";
};

function TypeVar(name) {
	if (!(this instanceof TypeVar)) {
		return new TypeVar(name);
	}

	this.name = name;
}

TypeVar.prototype.toString = function () {
	return mathMode(this.name);
};

function NameAdapter(name) {
	if (!(this instanceof NameAdapter)) {
		return new NameAdapter(name);
	}

	this.name = name;
}

NameAdapter.prototype.toString = function () {
	return this.name.toString();
};

// Collections
function List() {
	// A hack to turn `arguments` into a "real" array
	return Array.prototype.slice.call(arguments);
}

function Map(/*e1, e2, ...*/) {
	if (!(this instanceof Map)) {
		return new Map(Array.prototype.slice.call(arguments));
	}

	this.elements = arguments[0];
}

function Tuple2(e1, e2) {
	if (!(this instanceof Tuple2)) {
		return new Tuple2(e1, e2);
	}

	this.e1 = e1;
	this.e2 = e2;
}

// Names
function ConcreteName(namespace, name, position) {
	if (!(this instanceof ConcreteName)) {
		return new ConcreteName(namespace, name, position);
	}

	this.namespace = namespace;
	this.name = name;
	this.position = position;
}

ConcreteName.prototype.toString = function () {
	return this.namespace + '{' + mathMode(this.name) + '}@' + this.position;
};

function SymbolicName(namespace, name) {
	if (!(this instanceof SymbolicName)) {
		return new SymbolicName(namespace, name);
	}

	this.namespace = namespace;
	this.name = name;
}

SymbolicName.prototype.toString = function () {
	return this.namespace + '{' + mathMode(this.name) + '}';
};

function NameVar(name) {
	if (!(this instanceof NameVar)) {
		return new NameVar(name);
	}

	this.name = name;
}

NameVar.prototype.toString = function () {
	return mathMode(this.name);
};

// Facts
function CGDecl(scope, name) {
	if (!(this instanceof CGDecl)) {
		return new CGDecl(scope, name);
	}

	this.scope = scope;
	this.name = name;
}

CGDecl.prototype.toString = function () {
	return this.name + ' <span data-toggle="tooltip" title="CGDecl"><-</span> ' + this.scope;
};

function CGRef(name, scope) {
	if (!(this instanceof CGRef)) {
		return new CGRef(name, scope);
	}

	this.name = name;
	this.scope = scope;
}

CGRef.prototype.toString = function () {
	return this.name + ' <span data-toggle="tooltip" title="CGRef">-></span> ' + this.scope;
};

function CResolve(name1, name2) {
	if (!(this instanceof CResolve)) {
		return new CResolve(name1, name2);
	}

	this.name1 = name1;
	this.name2 = name2;
}

CResolve.prototype.toString = function () {
	return this.name1 + ' <span data-toggle="tooltip" title="CResolve">|-></span> ' + this.name2;
};

function CGDirectEdge(scope1, label, scope2) {
	if (!(this instanceof CGDirectEdge)) {
		return new CGDirectEdge(scope1, label, scope2);
	}

	this.scope1 = scope1;
	this.label = label;
	this.scope2 = scope2;
}

CGDirectEdge.prototype.toString = function () {
	return this.scope1 + ' <span data-toggle="tooltip" title="CGDirectEdge">-' + this.label + '-></span> ' + this.scope2;
};

// Label
function Label(name) {
	if (!(this instanceof Label)) {
		return new Label(name);
	}

	this.name = name;
}

Label.prototype.toString = function () {
	return this.name;
};

function AImport(name1, name2) {
	if (!(this instanceof AImport)) {
		return new AImport(name1, name2);
	}

	this.name1 = name1;
	this.name2 = name2;
}

AImport.prototype.toString = function () {
	return this.name1 + " import " + this.name2;
};

// Constraints
function NewScope(variable) {
	if (!(this instanceof NewScope)) {
		return new NewScope(variable);
	}

	this.variable = variable;
}

NewScope.prototype.toString = function () {
	return 'new ' + this.variable;
};

function CTrue() {
	if (!(this instanceof CTrue)) {
		return new CTrue();
	}
}

CTrue.prototype.toString = function () {
	return "&#8868;";
};

function CTypeOf(name, type) {
	if (!(this instanceof CTypeOf)) {
		return new CTypeOf(name, type);
	}

	this.name = name;
	this.type = type;
}

CTypeOf.prototype.toString = function () {
	return this.name + ' <span data-toggle="tooltip" title="CTypeOf">:</span> ' + this.type;
};

// CSubtype
function CSubtype(t1, t2) {
	if (!(this instanceof CSubtype)) {
		return new CSubtype(t1, t2);
	}

	this.t1 = t1;
	this.t2 = t2;
}

CSubtype.prototype.toString = function () {
	return this.t1 + ' <span data-toggle="tooltip" title="CSubtype">&lt;?</span> ' + this.t2;
};

// FSubtype
function FSubtype(t1, t2) {
	if (!(this instanceof FSubtype)) {
		return new FSubtype(t1, t2);
	}

	this.t1 = t1;
	this.t2 = t2;
}

FSubtype.prototype.toString = function () {
	return this.t1 + ' <span data-toggle="tooltip" title="FSubtype">&lt;!</span> ' + this.t2;
};

// CEqual
function CEqual(type1, type2) {
	if (!(this instanceof CEqual)) {
		return new CEqual(type1, type2);
	}

	this.type1 = type1;
	this.type2 = type2;
}

CEqual.prototype.toString = function () {
	return this.type1 + ' <span data-toggle="tooltip" title="CEqual">&equiv;</span> ' + this.type2;
};

// CInequal
function CInequal(type1, type2) {
	if (!(this instanceof CInequal)) {
		return new CInequal(type1, type2);
	}

	this.type1 = type1;
	this.type2 = type2;
}

CInequal.prototype.toString = function () {
	return this.type1 + ' <span data-toggle="tooltip" title="CInequal">&ne;</span> ' + this.type2;
};

// Associate a name with a scope (fact)
function CGAssoc(name, scope) {
	if (!(this instanceof CGAssoc)) {
		return new CGAssoc(name, scope);
	}

	this.name = name;
	this.scope = scope;
}

CGAssoc.prototype.toString = function () {
	return this.name + ' <span data-toggle="tooltip" title="CGAssoc">===></span> ' + this.scope;
};

// Associate a name with a scope (constraint)
function CAssoc(name, scope) {
	if (!(this instanceof CAssoc)) {
		return new CAssoc(name, scope);
	}

	this.name = name;
	this.scope = scope;
}

CAssoc.prototype.toString = function () {
	return this.name + ' <span data-toggle="tooltip" title="CAssoc">?===></span> ' + this.scope;
};

// Import the scope associated to a name
function CGNamedEdge(scope, label, name) {
	if (!(this instanceof CGNamedEdge)) {
		return new CGNamedEdge(scope, label, name);
	}

	this.scope = scope;
	this.label = label;
	this.name = name;
}

CGNamedEdge.prototype.toString = function () {
	return this.name + ' <span data-toggle="tooltip" title="CGNamedEdge"><=' + this.label + '=</span> ' + this.scope;
};

// CDistinct
function CDistinct(names) {
	if (!(this instanceof CDistinct)) {
		return new CDistinct(names);
	}

	this.names = names;
}

CDistinct.prototype.toString = function () {
	return "!" + this.names;
};

// Declarations
function Declarations(scope, namespace) {
	if (!(this instanceof Declarations)) {
		return new Declarations(scope, namespace);
	}

	this.scope = scope;
	this.namespace = namespace;
}

Declarations.prototype.toString = function () {
	return "D(" + this.scope + ")/" + this.namespace;
};

// CGenRecurse the generation
function CGenRecurse(name, pattern, scopes, type, sort) {
	if (!(this instanceof CGenRecurse)) {
		return new CGenRecurse(name, pattern, scopes, type, sort);
	}

	if (name == "Default") {
		this.name = "";
	} else {
		this.name = name;
	}
	
	this.pattern = pattern;
	this.scopes = scopes;
	this.type = type;
	this.sort = sort;
}

CGenRecurse.prototype.toString = function () {
	var scopes = this.scopes
		.map(function (e) {
			return e.toString();
		})
		.join(", ");

	if (this.type == None) {
		return this.name + " &#x27e6; " + this.pattern + " ^ (" + scopes + ") &#x27e7; @ " + this.sort;
	} else {
		return this.name + " &#x27e6; " + this.pattern + " ^ (" + scopes + ") : " + this.type + " &#x27e7; @ " + this.sort;
	}
};

// Name equality
function Eq(n1, n2) {
	if (!(this instanceof Eq)) {
		return new Eq(n1, n2);
	}

	this.n1 = n1;
	this.n2 = n2;
}

Eq.prototype.toString = function () {
	return this.n1 + " == " + this.n2;
};

// Name disequality
function Diseq(n1, n2) {
	if (!(this instanceof Diseq)) {
		return new Diseq(n1, n2);
	}

	this.n1 = n1;
	this.n2 = n2;
}

Diseq.prototype.toString = function () {
	return this.n1 + " != " + this.n2;
};

function PatternNameAdapter(name) {
	return name;
}

function TypeNameAdapter(type) {
	return type;
}

/**
 * Formatting
 */
function mathMode(string) {
	return string.replace(/([0-9]+)/, "<sub>$1</sub>");
}

/**
 * A generator that returns ever increasing numbers
 */
function* idGeneratorFactory() {
	var counter = 0;

	while (true) {
		yield counter++;
	}
}

var idGenerator = idGeneratorFactory();

function nextId() {
	return idGenerator.next().value;
}

/**
 * Draw a node and recursively draw its children
 */
function populate(node, nodes, edges) {
	var id = nextId();

	if (node instanceof Var) {
		var name = '<em style="color: #d9534f;">' + node.name + '</em>';
	} else if (node instanceof As) {
		if (node.term instanceof Var) {
			var name = '<em style="color: #d9534f;">' + node.alias + '@' + node.term.name + '</em>';
		} else {
			var name = '<em style="color: #d9534f;">' + node.alias + '</em>@' + node.term.name;
		}
	} else {
		if (node == undefined || node.name == undefined) {
			console.log(node);
		}

		var name = node.name;
	}

	var label = getLabel(
		name,
		node.types ? node.types.toString() : '',
		node.scopes ? node.scopes.toString() : ''
	);

	nodes.push({
		id: id,
		image: label,
		shape: 'image'
	});

	for (child in node.children) {
		edges.push({
			from: id,
			to: populate(node.children[child], nodes, edges),
		});
	}

	return id;
}

function getLabel(name, type, scope) {
	 var html =
	 	'<svg xmlns="http://www.w3.org/2000/svg" width="250" height="65">' +
			'<rect x="0" y="0" width="100%" height="100%" fill="#ffffff" stroke="#ffffff"></rect>' +
			'<foreignObject x="20" y="10" width="100%" height="100%">' +
				'<div xmlns="http://www.w3.org/1999/xhtml" style="font-family: Arial; text-align: center;">' +
					'<span>' + name + ' <sup>' + type + ', ' + scope + '</sup></span>' +
				'</div>' +
			'</foreignObject>' +
		'</svg>';


    var domUrl = window.URL || window.webkitURL || window;
    var image = new Image();
    var blob = new Blob([html], {type: 'image/svg+xml;charset=utf-8'});
    var url = domUrl.createObjectURL(blob);

    return url;
}

function displayState(state) {
	// Statistics
	$('#holes').html(state.holes());
	$('#size').html(state.size());

	// Proper constraints
	var ul = $('#constraints');
	ul.empty();

	for (i in state.constraints) {
		var constraint = state.constraints[i];
		var li = $('<li/>').html(constraint.toString());

		ul.append(li);
	}

	// Types
	var ul = $('#types');
	ul.empty();

	for (i in state.typeEnv.map.elements) {
		var binding = state.typeEnv.map.elements[i];
		var li = $('<li/>').html(binding.name + ": " + binding.type);

		ul.append(li);
	}

	// Resolution
	var ul = $('#resolution');
	ul.empty();

	for (i in state.resolution.map.elements) {
		var tuple2 = state.resolution.map.elements[i];
		var li = $('<li/>').html(tuple2.e1 + " |-> " + tuple2.e2);

		ul.append(li);
	}

	// Subtype relation
	var ul = $('#subtypes');
	ul.empty();

	for (i in state.subtypes.bindings) {
		var binding = state.subtypes.bindings[i];
		var li = $('<li/>').html(binding.toString());

		ul.append(li);
	}
}

/**
 * Draw
 */
var current = 0;

$(function () {
	var fragments = [];

	$(document).keydown(function (event) {
		if (event.target.tagName.toLowerCase() == 'textarea') {
			return;
		}

		switch (event.which) {
			case 37: // Left
				prev();
			break;

			case 39: // Right
				next();
			break;
		}
	})

	$('#prev').click(prev);

	var prev = function () {
		if (fragments.length > 1) {
			current = ((current-1) % fragments.length + fragments.length) % fragments.length;

			redraw();
		}
	};

	$('#next').click(next);

	var next = function () {
		if (fragments.length > 1) {
			current = (current+1) % fragments.length;

			redraw();
		}
	};


	$('#term').change(function () {
		current = 0;
		
		reread();
		redraw();
	});

	var container = $('#tree')[0];

	var options = {
		layout: {
			hierarchical: {
				sortMethod: "directed"
			}
		},
		edges: {
			smooth: false,
			arrows: {to : true }
		}
	};

	function not(f) {
		return function (x) {
			return !f(x);
		};
	}

	function isEmpty(str) {
		return !str || !str.length;
	}

	function currentFragment(current) {
		return eval(fragments[current]);
	}

	function reread() {
		fragments = $('#term').val().split('\n').filter(not(isEmpty));
	}
	
	function redraw() {
		var rule = currentFragment(current);

		$('.current').text(current+1);
		$('.total').text(fragments.length);

		if (rule !== undefined && rule instanceof Program) {
			displayPattern(rule.pattern);
			displayState(rule);
		} else {
			console.log('I dont know what to do!');
		}
	}

	function displayPattern(term) {
		var nodes = [];
		var edges = [];

		populate(term, nodes, edges);

		var data = {
			nodes: nodes,
			edges: edges
		};

		new vis.Network(container, data, options);
	}
});
