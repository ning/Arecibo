/* Copyright 2008-9 Google Inc. All Rights Reserved. */
(function() {
    function g(a) {
        throw a;
    }

    var i = true,j = null,k = false,aa = encodeURIComponent,ba = window,ca = Object,l = Error,m = undefined,da = parseInt,n = String,p = document,ea = decodeURIComponent,q = Array;

    function fa(a, b) {
        return a.currentTarget = b
    }

    function ga(a, b) {
        return a.keyCode = b
    }

    function ha(a, b) {
        return a.type = b
    }

    function ia(a, b) {
        return a.length = b
    }

    function ja(a, b) {
        return a.className = b
    }

    function ka(a, b) {
        return a.target = b
    }

    function la(a, b) {
        return a.href = b
    }

    var ma = "appendChild",s = "push",na = "slice",u = "replace",oa = "nodeType",pa = "concat",qa = "value",v = "indexOf",ra = "dispatchEvent",sa = "capture",ta = "currentTarget",ua = "screenX",va = "screenY",wa = "getBoxObjectFor",xa = "charCode",ya = "createElement",w = "keyCode",za = "firstChild",Aa = "forEach",Ba = "handleEvent",x = "type",Ca = "defaultView",Da = "name",Ea = "contentWindow",Fa = "documentElement",Ga = "stop",y = "toString",z = "length",Ha = "propertyIsEnumerable",A = "prototype",Ia = "setTimeout",Ja = "document",C = "split",Ka = "location",La = "hasOwnProperty",
            Ma = "style",D = "body",Na = "history",Oa = "removeChild",Pa = "target",E = "call",Qa = "href",Ra = "substring",Sa = "every",Ta = "contains",G = "apply",H = "parentNode",Ua = "join",Va = "toLowerCase",I,J = this,Wa = function(a, b, c) {
        a = a[C](".");
        c = c || J;
        !(a[0]in c) && c.execScript && c.execScript("var " + a[0]);
        for (var d; a[z] && (d = a.shift());)if (!a[z] && b !== m)c[d] = b;
        else c = c[d] ? c[d] : (c[d] = {})
    },Xa = function(a, b) {
        a = a[C](".");
        b = b || J;
        for (var c; c = a.shift();)if (b[c])b = b[c];
        else return j;
        return b
    },Ya = function() {
    },Za = function(a) {
        a.Q = function() {
            return a.Qb ||
                   (a.Qb = new a)
        }
    },$a = function(a) {
        var b = typeof a;
        if (b == "object")if (a) {
            if (a instanceof q || !(a instanceof ca) && ca[A][y][E](a) == "[object Array]" || typeof a[z] == "number" && typeof a.splice != "undefined" && typeof a[Ha] != "undefined" && !a[Ha]("splice"))return"array";
            if (!(a instanceof ca) && (ca[A][y][E](a) == "[object Function]" || typeof a[E] != "undefined" && typeof a[Ha] != "undefined" && !a[Ha]("call")))return"function"
        }
        else return"null";
        else if (b == "function" && typeof a[E] == "undefined")return"object";
        return b
    },K = function(a) {
        return $a(a) ==
               "array"
    },ab = function(a) {
        var b = $a(a);
        return b == "array" || b == "object" && typeof a[z] == "number"
    },L = function(a) {
        return typeof a == "string"
    },bb = function(a) {
        return $a(a) == "function"
    },cb = function(a) {
        a = $a(a);
        return a == "object" || a == "array" || a == "function"
    },fb = function(a) {
        if (a[La] && a[La](db))return a[db];
        a[db] || (a[db] = ++eb);
        return a[db]
    },db = "closure_hashCode_" + Math.floor(Math.random() * 2147483648)[y](36),eb = 0,gb = function(a) {
        var b = $a(a);
        if (b == "object" || b == "array") {
            if (a.o)return a.o[E](a);
            b = b == "array" ? [] : {};
            for (var c in a)b[c] =
                            gb(a[c]);
            return b
        }
        return a
    },hb = function(a, b) {
        var c = b || J;
        if (arguments[z] > 2) {
            var d = q[A][na][E](arguments, 2);
            return function() {
                var e = q[A][na][E](arguments);
                q[A].unshift[G](e, d);
                return a[G](c, e)
            }
        }
        else return function() {
            return a[G](c, arguments)
        }
    },ib = function(a) {
        var b = q[A][na][E](arguments, 1);
        return function() {
            var c = q[A][na][E](arguments);
            c.unshift[G](c, b);
            return a[G](this, c)
        }
    },jb = Date.now || function() {
        return(new Date).getTime()
    },M = function(a, b) {
        function c() {
        }

        c.prototype = b[A];
        a.f = b[A];
        a.prototype = new c;
        a[A].constructor = a
    };
    var kb = function(a, b, c) {
        if (a[v])return a[v](b, c);
        if (q[v])return q[v](a, b, c);
        for (c = c == j ? 0 : c < 0 ? Math.max(0, a[z] + c) : c; c < a[z]; c++)if (c in a && a[c] === b)return c;
        return-1
    },lb = function(a, b, c) {
        if (a[Aa])a[Aa](b, c);
        else if (q[Aa])q[Aa](a, b, c);
        else for (var d = a[z],e = L(a) ? a[C]("") : a,f = 0; f < d; f++)f in e && b[E](c, e[f], f, a)
    },mb = function(a, b) {
        if (a[Ta])return a[Ta](b);
        return kb(a, b) > -1
    },nb = function(a, b) {
        b = kb(a, b);
        var c;
        if (c = b != -1)q[A].splice[E](a, b, 1)[z] == 1;
        return c
    },ob = function(a) {
        if (K(a))return a[pa]();
        else {
            for (var b =
                    [],c = 0,d = a[z]; c < d; c++)b[c] = a[c];
            return b
        }
    },pb = function(a) {
        for (var b = 1; b < arguments[z]; b++) {
            var c = arguments[b];
            if (ab(c)) {
                c = c;
                c = K(c) ? c[pa]() : ob(c);
                a[s][G](a, c)
            }
            else a[s](c)
        }
    },rb = function(a) {
        return q[A].splice[G](a, qb(arguments, 1))
    },qb = function(a, b, c) {
        return arguments[z] <= 2 ? q[A][na][E](a, b) : q[A][na][E](a, b, c)
    };
    var sb,tb = function(a) {
        return(a = a.className) && typeof a[C] == "function" ? a[C](" ") : []
    },ub = function(a) {
        var b = tb(a),c;
        c = qb(arguments, 1);
        for (var d = 0,e = 0; e < c[z]; e++)if (!mb(b, c[e])) {
            b[s](c[e]);
            d++
        }
        c = d == c[z];
        ja(a, b[Ua](" "));
        return c
    },vb = function(a) {
        var b = tb(a),c;
        c = qb(arguments, 1);
        for (var d = 0,e = 0; e < b[z]; e++)if (mb(c, b[e])) {
            rb(b, e--, 1);
            d++
        }
        c = d == c[z];
        ja(a, b[Ua](" "));
        return c
    };
    var wb = function(a, b) {
        this.x = a !== m ? a : 0;
        this.y = b !== m ? b : 0
    };
    wb[A].o = function() {
        return new wb(this.x, this.y)
    };
    wb[A].toString = function() {
        return"(" + this.x + ", " + this.y + ")"
    };
    var xb = function(a, b, c) {
        for (var d in a)b[E](c, a[d], d, a)
    },yb = function(a) {
        var b = [],c = 0;
        for (var d in a)b[c++] = a[d];
        return b
    },zb = function(a) {
        var b = [],c = 0;
        for (var d in a)b[c++] = d;
        return b
    },Ab = function(a, b, c) {
        if (b in a)return a[b];
        return c
    },Bb = ["constructor","hasOwnProperty","isPrototypeOf","propertyIsEnumerable","toLocaleString","toString","valueOf"],Cb = function(a) {
        for (var b,c,d = 1; d < arguments[z]; d++) {
            c = arguments[d];
            for (b in c)a[b] = c[b];
            for (var e = 0; e < Bb[z]; e++) {
                b = Bb[e];
                if (ca[A][La][E](c, b))a[b] = c[b]
            }
        }
    },
            Db = function() {
                var a = arguments[z];
                if (a == 1 && K(arguments[0]))return Db[G](j, arguments[0]);
                if (a % 2)g(l("Uneven number of arguments"));
                for (var b = {},c = 0; c < a; c += 2)b[arguments[c]] = arguments[c + 1];
                return b
            };
    var Eb = function(a) {
        for (var b = 1; b < arguments[z]; b++) {
            var c = n(arguments[b])[u](/\$/g, "$$$$");
            a = a[u](/\%s/, c)
        }
        return a
    },Fb = function(a) {
        return a[u](/^[\s\xa0]+|[\s\xa0]+$/g, "")
    },Gb = /^[a-zA-Z0-9\-_.!~*'()]*$/,Hb = function(a) {
        a = n(a);
        if (!Gb.test(a))return aa(a);
        return a
    },Ib = function(a) {
        return ea(a[u](/\+/g, " "))
    },Ob = function(a, b) {
        if (b)return a[u](Jb, "&amp;")[u](Kb, "&lt;")[u](Lb, "&gt;")[u](Mb, "&quot;");
        else {
            if (!Nb.test(a))return a;
            if (a[v]("&") != -1)a = a[u](Jb, "&amp;");
            if (a[v]("<") != -1)a = a[u](Kb, "&lt;");
            if (a[v](">") !=
                -1)a = a[u](Lb, "&gt;");
            if (a[v]('"') != -1)a = a[u](Mb, "&quot;");
            return a
        }
    },Jb = /&/g,Kb = /</g,Lb = />/g,Mb = /\"/g,Nb = /[&<>\"]/,Pb = function(a, b) {
        return a[v](b) != -1
    },Rb = function(a, b) {
        var c = 0;
        a = Fb(n(a))[C](".");
        b = Fb(n(b))[C](".");
        for (var d = Math.max(a[z], b[z]),e = 0; c == 0 && e < d; e++) {
            var f = a[e] || "",h = b[e] || "",o = new RegExp("(\\d*)(\\D*)", "g"),r = new RegExp("(\\d*)(\\D*)", "g");
            do{
                var t = o.exec(f) || ["","",""],B = r.exec(h) || ["","",""];
                if (t[0][z] == 0 && B[0][z] == 0)break;
                c = t[1][z] == 0 ? 0 : da(t[1], 10);
                var F = B[1][z] == 0 ? 0 : da(B[1], 10);
                c = Qb(c, F) || Qb(t[2][z] == 0, B[2][z] == 0) || Qb(t[2], B[2])
            } while (c == 0)
        }
        return c
    },Qb = function(a, b) {
        if (a < b)return-1;
        else if (a > b)return 1;
        return 0
    };
    jb();
    var Sb,Tb,Ub,Vb,Wb,Xb,Yb,Zb,$b,ac,bc = function() {
        return J.navigator ? J.navigator.userAgent : j
    },cc = function() {
        return J.navigator
    };
    (function() {
        Xb = Wb = Vb = Ub = Tb = Sb = k;
        var a;
        if (a = bc()) {
            var b = cc();
            Sb = a[v]("Opera") == 0;
            Tb = !Sb && a[v]("MSIE") != -1;
            Vb = (Ub = !Sb && a[v]("WebKit") != -1) && a[v]("Mobile") != -1;
            Xb = (Wb = !Sb && !Ub && b.product == "Gecko") && b.vendor == "Camino"
        }
    })();
    var dc = Sb,O = Tb,P = Wb,Q = Ub,ec = Vb,fc = function() {
        var a = cc();
        return a && a.platform || ""
    }();
    (function() {
        Yb = Pb(fc, "Mac");
        Zb = Pb(fc, "Win");
        $b = Pb(fc, "Linux");
        ac = !!cc() && Pb(cc().appVersion || "", "X11")
    })();
    var gc = Yb,hc = ac,ic = function() {
        var a = "",b;
        if (dc && J.opera) {
            a = J.opera.version;
            a = typeof a == "function" ? a() : a
        }
        else {
            if (P)b = /rv\:([^\);]+)(\)|;)/;
            else if (O)b = /MSIE\s+([^\);]+)(\)|;)/;
            else if (Q)b = /WebKit\/(\S+)/;
            if (b)a = (a = b.exec(bc())) ? a[1] : ""
        }
        return a
    }(),jc = {},R = function(a) {
        return jc[a] || (jc[a] = Rb(ic, a) >= 0)
    };
    var mc = function(a) {
        return a ? new kc(lc(a)) : sb || (sb = new kc)
    },S = function(a) {
        return L(a) ? p.getElementById(a) : a
    },nc = function(a, b, c) {
        var d = p;
        c = c || d;
        a = a && a != "*" ? a[Va]() : "";
        if (c.querySelectorAll && (a || b) && (!Q || d.compatMode == "CSS1Compat" || R("528")))b = c.querySelectorAll(a + (b ? "." + b : ""));
        else if (b && c.getElementsByClassName) {
            d = c.getElementsByClassName(b);
            if (a) {
                c = {};
                for (var e = 0,f = 0,h; h = d[f]; f++)if (a == h.nodeName[Va]())c[e++] = h;
                ia(c, e);
                b = c
            }
            else b = d
        }
        else {
            d = c.getElementsByTagName(a || "*");
            if (b) {
                c = {};
                for (f = e = 0; h = d[f]; f++) {
                    a =
                    h.className;
                    if (typeof a[C] == "function" && mb(a[C](" "), b))c[e++] = h
                }
                ia(c, e);
                b = c
            }
            else b = d
        }
        return b
    },pc = function(a, b) {
        xb(b, function(c, d) {
            if (d == "style")a[Ma].cssText = c;
            else if (d == "class")ja(a, c);
            else if (d == "for")a.htmlFor = c;
                else if (d in oc)a.setAttribute(oc[d], c);
                    else a[d] = c
        })
    },oc = {cellpadding:"cellPadding",cellspacing:"cellSpacing",colspan:"colSpan",rowspan:"rowSpan",valign:"vAlign",height:"height",width:"width",usemap:"useMap",frameborder:"frameBorder",type:"type"},rc = function(a) {
        return a ? qc(a) : ba
    },qc =
            function(a) {
                if (a.parentWindow)return a.parentWindow;
                if (Q && !R("500") && !ec) {
                    var b = a[ya]("script");
                    b.innerHTML = "document.parentWindow=window";
                    var c = a[Fa];
                    c[ma](b);
                    c[Oa](b);
                    return a.parentWindow
                }
                return a[Ca]
            },tc = function(a, b) {
        var c = b[0],d = b[1];
        if (O && d && (d[Da] || d[x])) {
            c = ["<",c];
            d[Da] && c[s](' name="', Ob(d[Da]), '"');
            if (d[x]) {
                c[s](' type="', Ob(d[x]), '"');
                d = gb(d);
                delete d[x]
            }
            c[s](">");
            c = c[Ua]("")
        }
        var e = a[ya](c);
        if (d)if (L(d))ja(e, d);
        else pc(e, d);
        if (b[z] > 2) {
            function f(h) {
                if (h)e[ma](L(h) ? a.createTextNode(h) :
                            h)
            }

            for (d = 2; d < b[z]; d++) {
                c = b[d];
                ab(c) && !(cb(c) && c[oa] > 0) ? lb(sc(c) ? ob(c) : c, f) : f(c)
            }
        }
        return e
    },uc = function() {
        return tc(p, arguments)
    },vc = function(a, b) {
        a[ma](b)
    },wc = function(a) {
        return a && a[H] ? a[H][Oa](a) : j
    },xc = Q && R("522"),yc = function(a, b) {
        if (typeof a[Ta] != "undefined" && !xc && b[oa] == 1)return a == b || a[Ta](b);
        if (typeof a.compareDocumentPosition != "undefined")return a == b || Boolean(a.compareDocumentPosition(b) & 16);
        for (; b && a != b;)b = b[H];
        return b == a
    },lc = function(a) {
        return a[oa] == 9 ? a : a.ownerDocument || a[Ja]
    },zc = function(a) {
        return a =
               Q ? a[Ja] || a[Ea][Ja] : a.contentDocument || a[Ea][Ja]
    },Ac = function(a, b) {
        if ("textContent"in a)a.textContent = b;
        else if (a[za] && a[za][oa] == 3) {
            for (; a.lastChild != a[za];)a[Oa](a.lastChild);
            a[za].data = b
        }
        else {
            for (var c; c = a[za];)a[Oa](c);
            c = lc(a);
            a[ma](c.createTextNode(b))
        }
    },Bc = function(a, b) {
        if (b)a.tabIndex = 0;
        else a.removeAttribute("tabIndex")
    },sc = function(a) {
        if (a && typeof a[z] == "number")if (cb(a))return typeof a.item == "function" || typeof a.item == "string";
        else if (bb(a))return typeof a.item == "function";
        return k
    },kc = function(a) {
        this.C =
        a || J[Ja] || p
    };
    kc[A].P = function(a) {
        return L(a) ? this.C.getElementById(a) : a
    };
    kc[A].createElement = function(a) {
        return this.C[ya](a)
    };
    kc[A].createTextNode = function(a) {
        return this.C.createTextNode(a)
    };
    var Cc = function(a) {
        return a.C.compatMode == "CSS1Compat"
    };
    kc[A].appendChild = vc;
    kc[A].contains = yc;
    var Dc = "StopIteration"in J ? J.StopIteration : l("StopIteration"),Ec = function() {
    };
    Ec[A].next = function() {
        g(Dc)
    };
    Ec[A].yb = function() {
        return this
    };
    var Fc = function(a) {
        if (typeof a.G == "function")return a.G();
        if (L(a))return a[C]("");
        if (ab(a)) {
            for (var b = [],c = a[z],d = 0; d < c; d++)b[s](a[d]);
            return b
        }
        return yb(a)
    },Gc = function(a, b, c) {
        if (typeof a[Aa] == "function")a[Aa](b, c);
        else if (ab(a) || L(a))lb(a, b, c);
        else {
            var d;
            if (typeof a.R == "function")d = a.R();
            else if (typeof a.G != "function")if (ab(a) || L(a)) {
                d = [];
                for (var e = a[z],f = 0; f < e; f++)d[s](f);
                d = d
            }
            else d = zb(a);
            else d = void 0;
            e = Fc(a);
            f = e[z];
            for (var h = 0; h < f; h++)b[E](c, e[h], d && d[h], a)
        }
    };
    var Ic = function(a) {
        this.l = {};
        this.b = [];
        var b = arguments[z];
        if (b > 1) {
            if (b % 2)g(l("Uneven number of arguments"));
            for (var c = 0; c < b; c += 2)this.J(arguments[c], arguments[c + 1])
        }
        else a && Hc(this, a)
    };
    I = Ic[A];
    I.a = 0;
    I.Ka = 0;
    I.G = function() {
        Jc(this);
        for (var a = [],b = 0; b < this.b[z]; b++)a[s](this.l[this.b[b]]);
        return a
    };
    I.R = function() {
        Jc(this);
        return this.b[pa]()
    };
    I.u = function(a) {
        return Kc(this.l, a)
    };
    I.remove = function(a) {
        if (Kc(this.l, a)) {
            delete this.l[a];
            this.a--;
            this.Ka++;
            this.b[z] > 2 * this.a && Jc(this);
            return i
        }
        return k
    };
    var Jc = function(a) {
        if (a.a != a.b[z]) {
            for (var b = 0,c = 0; b < a.b[z];) {
                var d = a.b[b];
                if (Kc(a.l, d))a.b[c++] = d;
                b++
            }
            ia(a.b, c)
        }
        if (a.a != a.b[z]) {
            var e = {};
            for (c = b = 0; b < a.b[z];) {
                d = a.b[b];
                if (!Kc(e, d)) {
                    a.b[c++] = d;
                    e[d] = 1
                }
                b++
            }
            ia(a.b, c)
        }
    };
    Ic[A].v = function(a, b) {
        if (Kc(this.l, a))return this.l[a];
        return b
    };
    Ic[A].J = function(a, b) {
        if (!Kc(this.l, a)) {
            this.a++;
            this.b[s](a);
            this.Ka++
        }
        this.l[a] = b
    };
    var Hc = function(a, b) {
        var c;
        if (b instanceof Ic) {
            c = b.R();
            b = b.G()
        }
        else {
            c = zb(b);
            b = yb(b)
        }
        for (var d = 0; d < c[z]; d++)a.J(c[d], b[d])
    };
    Ic[A].o = function() {
        return new Ic(this)
    };
    Ic[A].yb = function(a) {
        Jc(this);
        var b = 0,c = this.b,d = this.l,e = this.Ka,f = this,h = new Ec;
        h.next = function() {
            for (; 1;) {
                if (e != f.Ka)g(l("The map has changed since the iterator was created"));
                if (b >= c[z])g(Dc);
                var o = c[b++];
                return a ? o : d[o]
            }
        };
        return h
    };
    var Kc = function(a, b) {
        return ca[A][La][E](a, b)
    };
    var Lc = function(a) {
        var b = a[x];
        if (b === m)return j;
        switch (b[Va]()) {case "checkbox":case "radio":return a.checked ? a[qa] : j;case "select-one":b = a.selectedIndex;return a = b >= 0 ? a.options[b][qa] : j;case "select-multiple":b = [];for (var c,d = 0; c = a.options[d]; d++)c.selected && b[s](c[qa]);return a = b[z] ? b : j;default:return a[qa] !== m ? a[qa] : j}
    };
    var Mc = function() {
    };
    Mc[A].Pa = k;
    Mc[A].Rb = function() {
        return this.Pa
    };
    Mc[A].q = function() {
        if (!this.Pa) {
            this.Pa = i;
            this.d()
        }
    };
    Mc[A].d = function() {
    };
    var T = function(a, b) {
        ha(this, a);
        ka(this, b);
        fa(this, this[Pa])
    };
    M(T, Mc);
    T[A].d = function() {
        delete this[x];
        delete this[Pa];
        delete this[ta]
    };
    T[A].W = k;
    T[A].Y = i;
    T[A].preventDefault = function() {
        this.Y = k
    };
    var Nc = function(a, b) {
        a && this.H(a, b)
    };
    M(Nc, T);
    I = Nc[A];
    ka(I, j);
    I.relatedTarget = j;
    I.offsetX = 0;
    I.offsetY = 0;
    I.clientX = 0;
    I.clientY = 0;
    I.screenX = 0;
    I.screenY = 0;
    I.button = 0;
    ga(I, 0);
    I.charCode = 0;
    I.ctrlKey = k;
    I.altKey = k;
    I.shiftKey = k;
    I.metaKey = k;
    I.ga = j;
    I.H = function(a, b) {
        var c = ha(this, a[x]);
        ka(this, a[Pa] || a.srcElement);
        fa(this, b);
        if (b = a.relatedTarget) {
            if (P)try {
                b = b.nodeName && b
            }
            catch(d) {
            }
        }
        else if (c == "mouseover")b = a.fromElement;
        else if (c == "mouseout")b = a.toElement;
        this.relatedTarget = b;
        this.offsetX = a.offsetX !== m ? a.offsetX : a.layerX;
        this.offsetY = a.offsetY !== m ? a.offsetY : a.layerY;
        this.clientX = a.clientX !== m ? a.clientX : a.pageX;
        this.clientY = a.clientY !== m ? a.clientY : a.pageY;
        this.screenX = a[ua] || 0;
        this.screenY = a[va] || 0;
        this.button = a.button;
        ga(this, a[w] || 0);
        this.charCode =
        a[xa] || (c == "keypress" ? a[w] : 0);
        this.ctrlKey = a.ctrlKey;
        this.altKey = a.altKey;
        this.shiftKey = a.shiftKey;
        this.metaKey = a.metaKey;
        this.ga = a;
        delete this.Y;
        delete this.W
    };
    var Oc = O && !R("8");
    Nc[A].preventDefault = function() {
        this.Y = k;
        var a = this.ga;
        if (a.preventDefault)a.preventDefault();
        else {
            a.returnValue = k;
            if (Oc)try {
                if (a.ctrlKey || a[w] >= 112 && a[w] <= 123)ga(a, -1)
            }
            catch(b) {
            }
        }
    };
    Nc[A].d = function() {
        Nc.f.d[E](this);
        this.ga = j;
        ka(this, j);
        fa(this, j);
        this.relatedTarget = j
    };
    var U = function(a, b) {
        this.lb = b;
        this.F = [];
        if (a > this.lb)g(l("[goog.structs.SimplePool] Initial cannot be greater than max"));
        for (b = 0; b < a; b++)this.F[s](this.p ? this.p() : {})
    };
    M(U, Mc);
    U[A].p = j;
    U[A].hb = j;
    var Pc = function(a) {
        if (a.F[z])return a.F.pop();
        return a.p ? a.p() : {}
    },Rc = function(a, b) {
        a.F[z] < a.lb ? a.F[s](b) : Qc(a, b)
    },Qc = function(a, b) {
        if (a.hb)a.hb(b);
        else if (bb(b.q))b.q();
        else for (var c in b)delete b[c]
    };
    U[A].d = function() {
        U.f.d[E](this);
        for (var a = this.F; a[z];)Qc(this, a.pop());
        delete this.F
    };
    var Sc,Tc;
    (function() {
        Tc = (Sc = "ScriptEngine"in J && J.ScriptEngine() == "JScript") ? J.ScriptEngineMajorVersion() + "." + J.ScriptEngineMinorVersion() + "." + J.ScriptEngineBuildVersion() : "0"
    })();
    var Uc = Sc,Vc = Tc;
    var Wc = function() {
    },Xc = 0;
    I = Wc[A];
    I.t = 0;
    I.X = k;
    I.Ma = k;
    I.H = function(a, b, c, d, e, f) {
        if (bb(a))this.ib = i;
        else if (a && a[Ba] && bb(a[Ba]))this.ib = k;
        else g(l("Invalid listener argument"));
        this.oa = a;
        this.rb = b;
        this.src = c;
        ha(this, d);
        this.capture = !!e;
        this.Sa = f;
        this.Ma = k;
        this.t = ++Xc;
        this.X = k
    };
    I.handleEvent = function(a) {
        if (this.ib)return this.oa[E](this.Sa || this.src, a);
        return this.oa[Ba][E](this.oa, a)
    };
    var Yc,Zc,$c,ad,bd,cd,dd,ed,fd,gd,hd;
    (function() {
        var a = Uc && !(Rb(Vc, "5.7") >= 0);

        function b() {
            return{a:0,i:0}
        }

        function c() {
            return[]
        }

        var d;
        cd = function(N) {
            d = N
        };
        function e() {
            var N = function(De) {
                return d[E](N.src, N.t, De)
            };
            return N
        }

        function f() {
            return new Wc
        }

        function h() {
            return new Nc
        }

        if (a) {
            Yc = function() {
                return Pc(o)
            };
            Zc = function(N) {
                Rc(o, N)
            };
            $c = function() {
                return Pc(r)
            };
            ad = function(N) {
                Rc(r, N)
            };
            bd = function() {
                return Pc(t)
            };
            dd = function() {
                Rc(t, e())
            };
            ed = function() {
                return Pc(B)
            };
            fd = function(N) {
                Rc(B, N)
            };
            gd = function() {
                return Pc(F)
            };
            hd = function(N) {
                Rc(F,
                        N)
            };
            var o = new U(0, 600);
            o.p = b;
            var r = new U(0, 600);
            r.p = c;
            var t = new U(0, 600);
            t.p = e;
            var B = new U(0, 600);
            B.p = f;
            var F = new U(0, 600);
            F.p = h
        }
        else {
            Yc = b;
            Zc = Ya;
            $c = c;
            ad = Ya;
            bd = e;
            dd = Ya;
            ed = f;
            fd = Ya;
            gd = h;
            hd = Ya
        }
    })();
    var id = {},V = {},W = {},jd = {},X = function(a, b, c, d, e) {
        if (b)if (K(b)) {
            for (var f = 0; f < b[z]; f++)X(a, b[f], c, d, e);
            return j
        }
        else {
            d = !!d;
            var h = V;
            b in h || (h[b] = Yc());
            h = h[b];
            if (!(d in h)) {
                h[d] = Yc();
                h.a++
            }
            h = h[d];
            var o = fb(a),r;
            h.i++;
            if (h[o]) {
                r = h[o];
                for (f = 0; f < r[z]; f++) {
                    h = r[f];
                    if (h.oa == c && h.Sa == e) {
                        if (h.X)break;
                        return r[f].t
                    }
                }
            }
            else {
                r = h[o] = $c();
                h.a++
            }
            f = bd();
            f.src = a;
            h = ed();
            h.H(c, f, a, b, d, e);
            c = h.t;
            f.t = c;
            r[s](h);
            id[c] = h;
            W[o] || (W[o] = $c());
            W[o][s](h);
            if (a.addEventListener) {
                if (a == J || !a.fb)a.addEventListener(b, f, d)
            }
            else a.attachEvent(kd(b),
                    f);
            return c
        }
        else g(l("Invalid event type"))
    },ld = function(a, b, c, d, e) {
        if (K(b)) {
            for (var f = 0; f < b[z]; f++)ld(a, b[f], c, d, e);
            return j
        }
        a = X(a, b, c, d, e);
        id[a].Ma = i;
        return a
    },md = function(a, b, c, d, e) {
        if (K(b)) {
            for (var f = 0; f < b[z]; f++)md(a, b[f], c, d, e);
            return j
        }
        d = !!d;
        a:{
            f = V;
            if (b in f) {
                f = f[b];
                if (d in f) {
                    f = f[d];
                    a = fb(a);
                    if (f[a]) {
                        a = f[a];
                        break a
                    }
                }
            }
            a = j
        }
        if (!a)return k;
        for (f = 0; f < a[z]; f++)if (a[f].oa == c && a[f][sa] == d && a[f].Sa == e)return Y(a[f].t);
        return k
    },Y = function(a) {
        if (!id[a])return k;
        var b = id[a];
        if (b.X)return k;
        var c = b.src,
                d = b[x],e = b.rb,f = b[sa];
        if (c.removeEventListener) {
            if (c == J || !c.fb)c.removeEventListener(d, e, f)
        }
        else c.detachEvent && c.detachEvent(kd(d), e);
        c = fb(c);
        e = V[d][f][c];
        if (W[c]) {
            var h = W[c];
            nb(h, b);
            h[z] == 0 && delete W[c]
        }
        b.X = i;
        e.nb = i;
        nd(d, f, c, e);
        delete id[a];
        return i
    },nd = function(a, b, c, d) {
        if (!d.Ga)if (d.nb) {
            for (var e = 0,f = 0; e < d[z]; e++)if (d[e].X) {
                var h = d[e].rb;
                h.src = j;
                dd(h);
                fd(d[e])
            }
            else {
                if (e != f)d[f] = d[e];
                f++
            }
            ia(d, f);
            d.nb = k;
            if (f == 0) {
                ad(d);
                delete V[a][b][c];
                V[a][b].a--;
                if (V[a][b].a == 0) {
                    Zc(V[a][b]);
                    delete V[a][b];
                    V[a].a--
                }
                if (V[a].a ==
                    0) {
                    Zc(V[a]);
                    delete V[a]
                }
            }
        }
    },od = function(a, b, c) {
        var d = 0,e = a == j,f = b == j,h = c == j;
        c = !!c;
        if (e)xb(W, function(r) {
            for (var t = r[z] - 1; t >= 0; t--) {
                var B = r[t];
                if ((f || b == B[x]) && (h || c == B[sa])) {
                    Y(B.t);
                    d++
                }
            }
        });
        else {
            a = fb(a);
            if (W[a]) {
                a = W[a];
                for (e = a[z] - 1; e >= 0; e--) {
                    var o = a[e];
                    if ((f || b == o[x]) && (h || c == o[sa])) {
                        Y(o.t);
                        d++
                    }
                }
            }
        }
        return d
    },kd = function(a) {
        if (a in jd)return jd[a];
        return jd[a] = "on" + a
    },qd = function(a, b, c, d, e) {
        var f = 1;
        b = fb(b);
        if (a[b]) {
            a.i--;
            a = a[b];
            if (a.Ga)a.Ga++;
            else a.Ga = 1;
            try {
                for (var h = a[z],o = 0; o < h; o++) {
                    var r = a[o];
                    if (r &&
                        !r.X)f &= pd(r, e) !== k
                }
            }
            finally {
                a.Ga--;
                nd(c, d, b, a)
            }
        }
        return Boolean(f)
    },pd = function(a, b) {
        b = a[Ba](b);
        a.Ma && Y(a.t);
        return b
    };
    cd(function(a, b) {
        if (!id[a])return i;
        a = id[a];
        var c = a[x],d = V;
        if (!(c in d))return i;
        d = d[c];
        var e,f;
        if (O) {
            e = b || Xa("window.event");
            b = i in d;
            var h = k in d;
            if (b) {
                if (e[w] < 0 || e.returnValue != m)return i;
                a:{
                    var o = k;
                    if (e[w] == 0)try {
                        ga(e, -1);
                        break a
                    }
                    catch(r) {
                        o = i
                    }
                    if (o || e.returnValue == m)e.returnValue = i
                }
            }
            o = gd();
            o.H(e, this);
            e = i;
            try {
                if (b) {
                    for (var t = $c(),B = o[ta]; B; B = B[H])t[s](B);
                    f = d[i];
                    f.i = f.a;
                    for (var F = t[z] - 1; !o.W && F >= 0 && f.i; F--) {
                        fa(o, t[F]);
                        e &= qd(f, t[F], c, i, o)
                    }
                    if (h) {
                        f = d[k];
                        f.i = f.a;
                        for (F = 0; !o.W && F < t[z] && f.i; F++) {
                            fa(o,
                                    t[F]);
                            e &= qd(f, t[F], c, k, o)
                        }
                    }
                }
                else e = pd(a, o)
            }
            finally {
                if (t) {
                    ia(t, 0);
                    ad(t)
                }
                o.q();
                hd(o)
            }
            return e
        }
        f = new Nc(b, this);
        try {
            e = pd(a, f)
        }
        finally {
            f.q()
        }
        return e
    });
    var rd = function(a) {
        this.Nb = a
    };
    M(rd, Mc);
    var sd = new U(0, 100);
    rd[A].T = function(a, b, c, d, e) {
        if (K(b))for (var f = 0; f < b[z]; f++)this.T(a, b[f], c, d, e);
        else {
            a = X(a, b, c || this, d || k, e || this.Nb || this);
            if (this.b)this.b[a] = i;
            else if (this.la) {
                this.b = Pc(sd);
                this.b[this.la] = i;
                this.la = j;
                this.b[a] = i
            }
            else this.la = a
        }
        return this
    };
    var td = function(a) {
        if (a.b) {
            for (var b in a.b) {
                Y(b);
                delete a.b[b]
            }
            Rc(sd, a.b);
            a.b = j
        }
        else a.la && Y(a.la)
    };
    rd[A].d = function() {
        rd.f.d[E](this);
        td(this)
    };
    rd[A].handleEvent = function() {
        g(l("EventHandler.handleEvent not implemented"))
    };
    var ud = function() {
    };
    M(ud, Mc);
    I = ud[A];
    I.fb = i;
    I.Ja = j;
    I.Ya = function(a) {
        this.Ja = a
    };
    I.addEventListener = function(a, b, c, d) {
        X(this, a, b, c, d)
    };
    I.removeEventListener = function(a, b, c, d) {
        md(this, a, b, c, d)
    };
    I.dispatchEvent = function(a) {
        a = a;
        if (L(a))a = new T(a, this);
        else if (a instanceof T)ka(a, a[Pa] || this);
        else {
            var b = a;
            a = new T(a[x], this);
            Cb(a, b)
        }
        b = 1;
        var c,d = a[x],e = V;
        if (d in e) {
            e = e[d];
            d = i in e;
            var f;
            if (d) {
                c = [];
                for (f = this; f; f = f.Ja)c[s](f);
                f = e[i];
                f.i = f.a;
                for (var h = c[z] - 1; !a.W && h >= 0 && f.i; h--) {
                    fa(a, c[h]);
                    b &= qd(f, c[h], a[x], i, a) && a.Y != k
                }
            }
            if (k in e) {
                f = e[k];
                f.i = f.a;
                if (d)for (h = 0; !a.W && h < c[z] && f.i; h++) {
                    fa(a, c[h]);
                    b &= qd(f, c[h], a[x], k, a) && a.Y != k
                }
                else for (c = this; !a.W && c && f.i; c = c.Ja) {
                    fa(a, c);
                    b &= qd(f, c, a[x], k, a) && a.Y !=
                                                 k
                }
            }
            a = Boolean(b)
        }
        else a = i;
        return a
    };
    I.d = function() {
        ud.f.d[E](this);
        od(this);
        this.Ja = j
    };
    var wd = function(a, b) {
        this.ka = a || 1;
        this.ra = b || vd;
        this.La = hb(this.cc, this);
        this.Wa = jb()
    };
    M(wd, ud);
    wd[A].enabled = k;
    var vd = J.window;
    I = wd[A];
    I.e = j;
    I.setInterval = function(a) {
        this.ka = a;
        if (this.e && this.enabled) {
            this[Ga]();
            this.start()
        }
        else this.e && this[Ga]()
    };
    I.cc = function() {
        if (this.enabled) {
            var a = jb() - this.Wa;
            if (a > 0 && a < this.ka * 0.8)this.e = this.ra[Ia](this.La, this.ka - a);
            else {
                this[ra]("tick");
                if (this.enabled) {
                    this.e = this.ra[Ia](this.La, this.ka);
                    this.Wa = jb()
                }
            }
        }
    };
    I.start = function() {
        this.enabled = i;
        if (!this.e) {
            this.e = this.ra[Ia](this.La, this.ka);
            this.Wa = jb()
        }
    };
    I.stop = function() {
        this.enabled = k;
        if (this.e) {
            this.ra.clearTimeout(this.e);
            this.e = j
        }
    };
    I.d = function() {
        wd.f.d[E](this);
        this[Ga]();
        delete this.ra
    };
    var Cd = function(a, b, c, d) {
        if (a && !b)g(l("Can't use invisible history without providing a blank page."));
        var e;
        if (c)e = c;
        else {
            e = "history_state" + xd;
            p.write(Eb('<input type="text" name="%s" id="%s" style="display:none" />', e, e));
            e = S(e)
        }
        this.ja = e;
        this.n = c ? rc(lc(c)) : ba;
        this.zb = this.n[Ka][Qa][C]("#")[0] + "#";
        this.Aa = b;
        if (O && !b)this.Aa = ba[Ka].protocol == "https" ? "https:///" : 'javascript:""';
        this.e = new wd(150);
        this.L = !a;
        this.D = new rd(this);
        if (a || O && !yd) {
            if (d)a = d;
            else {
                a = "history_iframe" + xd;
                b = this.Aa ? 'src="' + Ob(this.Aa) +
                              '"' : "";
                p.write(Eb('<iframe id="%s" style="display:none" %s></iframe>', a, b));
                a = S(a)
            }
            this.Ba = a;
            this.wb = i
        }
        if (O && !yd) {
            this.D.T(this.n, "load", this.Vb);
            this.tb = this.Qa = k
        }
        this.L ? zd(this, Ad(this), i) : Bd(this, this.ja[qa]);
        xd++
    };
    M(Cd, ud);
    Cd[A].fa = k;
    Cd[A].Ha = k;
    Cd[A].na = j;
    var yd = O && p.documentMode >= 8 || P && R("1.9.2") || Q && R("532.1");
    I = Cd[A];
    I.U = j;
    I.d = function() {
        Cd.f.d[E](this);
        this.D.q();
        this.Z(k)
    };
    I.Z = function(a) {
        if (a != this.fa)if (O && !yd && !this.Qa)this.tb = a;
        else if (a) {
                if (dc)this.D.T(this.n[Ja], Dd, this.Yb);
                else P && this.D.T(this.n, "pageshow", this.Xb);
                if (yd && this.L) {
                    this.D.T(this.n, "hashchange", this.Wb);
                    this.fa = i;
                    this[ra](new Ed(Ad(this)))
                }
                else if (!O || this.Qa) {
                    this.D.T(this.e, "tick", this.qb);
                    this.fa = i;
                    if (!O)this.na = Ad(this);
                    this.e.start();
                    this[ra](new Ed(Ad(this)))
                }
            }
            else {
                this.fa = k;
                td(this.D);
                this.e[Ga]()
            }
    };
    I.Vb = function() {
        this.Qa = i;
        this.ja[qa] && Bd(this, this.ja[qa], i);
        this.Z(this.tb)
    };
    I.Xb = function(a) {
        if (a.ga.persisted) {
            this.Z(k);
            this.Z(i)
        }
    };
    I.Wb = function() {
        var a = Fd(this, this.n);
        Gd(this, a)
    };
    var Ad = function(a) {
        return a.U !== j ? a.U : a.L ? Fd(a, a.n) : Hd(a) || ""
    },Fd = function(a, b) {
        a = b[Ka][Qa];
        b = a[v]("#");
        return b < 0 ? "" : a[Ra](b + 1)
    },zd = function(a, b, c) {
        b = a.zb + (b || "");
        a = a.n[Ka];
        if (b != a[Qa])if (c)a[u](b);
        else la(a, b)
    },Bd = function(a, b, c, d) {
        if (a.wb || b != Hd(a)) {
            a.wb = k;
            b = Hb(b);
            if (O) {
                var e = zc(a.Ba);
                e.open("text/html", c ? "replace" : m);
                e.write(Eb("<title>%s</title><body>%s</body>", Ob(d || a.n[Ja].title), b));
                e.close()
            }
            else {
                b = a.Aa + "#" + b;
                if (a = a.Ba[Ea])if (c)a[Ka][u](b);
                else la(a[Ka], b)
            }
        }
    },Hd = function(a) {
        if (O) {
            a = zc(a.Ba);
            return a[D] ? Ib(a[D].innerHTML) : j
        }
        else {
            var b = a.Ba[Ea];
            if (b) {
                var c;
                try {
                    c = Ib(Fd(a, b))
                }
                catch(d) {
                    a.Ha || Id(a, i);
                    return j
                }
                a.Ha && Id(a, k);
                return c || j
            }
            else return j
        }
    };
    Cd[A].qb = function() {
        if (this.L) {
            var a = Fd(this, this.n);
            a != this.na && Gd(this, a)
        }
        if (!this.L || O && !yd) {
            a = Hd(this) || "";
            if (this.U == j || a == this.U) {
                this.U = j;
                a != this.na && Gd(this, a)
            }
        }
    };
    var Gd = function(a, b) {
        a.na = a.ja.value = b;
        if (a.L) {
            O && !yd && Bd(a, b);
            zd(a, b)
        }
        else Bd(a, b);
        a[ra](new Ed(Ad(a)))
    },Id = function(a, b) {
        if (a.Ha != b)a.e.setInterval(b ? 10000 : 150);
        a.Ha = b
    };
    Cd[A].Yb = function() {
        this.e[Ga]();
        this.e.start()
    };
    var Dd = ["mousedown","keydown","mousemove"],xd = 0,Ed = function(a) {
        T[E](this, "navigate");
        this.dc = a
    };
    M(Ed, T);
    var Jd,Kd,Ld,Md,Nd,Od;
    (function() {
        Od = Nd = Md = Ld = Kd = Jd = k;
        var a = bc();
        if (a)if (a[v]("Firefox") != -1)Jd = i;
        else if (a[v]("Camino") != -1)Kd = i;
            else if (a[v]("iPhone") != -1 || a[v]("iPod") != -1)Ld = i;
                else if (a[v]("Android") != -1)Md = i;
                    else if (a[v]("Chrome") != -1)Nd = i;
                        else if (a[v]("Safari") != -1)Od = i
    })();
    var Pd = function(a, b) {
        var c;
        a:{
            c = lc(a);
            if (c[Ca] && c[Ca].getComputedStyle)if (c = c[Ca].getComputedStyle(a, "")) {
                c = c[b];
                break a
            }
            c = j
        }
        return c || (a.currentStyle ? a.currentStyle[b] : j) || a[Ma][b]
    },Qd = function(a) {
        if (O)return a.offsetParent;
        var b = lc(a),c = Pd(a, "position"),d = c == "fixed" || c == "absolute";
        for (a = a[H]; a && a != b; a = a[H]) {
            c = Pd(a, "position");
            d = d && c == "static" && a != b[Fa] && a != b[D];
            if (!d && (a.scrollWidth > a.clientWidth || a.scrollHeight > a.clientHeight || c == "fixed" || c == "absolute"))return a
        }
        return j
    },Rd = function(a, b) {
        a[Ma].display =
        b ? "" : "none"
    };
    var Sd = function() {
    };
    Za(Sd);
    Sd[A].Ub = 0;
    Sd.Q();
    var Ud = function(a) {
        this.Hb = a || mc();
        this.Zb = Td
    };
    M(Ud, ud);
    Ud[A].Pb = Sd.Q();
    var Td = j,Vd = function(a, b) {
        switch (a) {case 1:return b ? "disable" : "enable";case 2:return b ? "highlight" : "unhighlight";case 4:return b ? "activate" : "deactivate";case 8:return b ? "select" : "unselect";case 16:return b ? "check" : "uncheck";case 32:return b ? "focus" : "blur";case 64:return b ? "open" : "close";default:}
        g(l("Invalid component state"))
    };
    I = Ud[A];
    I.Ta = j;
    I.Hb = j;
    I.Ca = k;
    I.g = j;
    I.Zb = j;
    I.mb = j;
    I.pa = j;
    I.O = j;
    I.va = j;
    I.fc = k;
    I.P = function() {
        return this.g
    };
    I.Ya = function(a) {
        if (this.pa && this.pa != a)g(l("Method not supported"));
        Ud.f.Ya[E](this, a)
    };
    I.ha = function() {
        this.O && lb(this.O, function(a) {
            a.Ca && a.ha()
        }, m);
        this.za && td(this.za);
        this.Ca = k
    };
    I.d = function() {
        Ud.f.d[E](this);
        this.Ca && this.ha();
        if (this.za) {
            this.za.q();
            delete this.za
        }
        this.O && lb(this.O, function(a) {
            a.q()
        }, m);
        !this.fc && this.g && wc(this.g);
        this.pa = this.mb = this.g = this.va = this.O = j
    };
    I.removeChild = function(a, b) {
        if (a) {
            var c = L(a) ? a : a.Ta || (a.Ta = ":" + (a.Pb.Ub++)[y](36));
            a = this.va && c ? Ab(this.va, c) || j : j;
            if (c && a) {
                var d = this.va;
                c in d && delete d[c];
                nb(this.O, a);
                if (b) {
                    a.ha();
                    a.g && wc(a.g)
                }
                b = a;
                if (b == j)g(l("Unable to set parent component"));
                b.pa = j;
                Ud.f.Ya[E](b, j)
            }
        }
        if (!a)g(l("Child is not in parent component"));
        return a
    };
    var Wd,Xd = function(a, b, c) {
        if (P || Wd)a.setAttribute("aria-" + b, c)
    };
    var Zd = function(a, b, c, d, e) {
        if (!O && !(Q && R("525")))return i;
        if (gc && e)return Yd(a);
        if (e && !d)return k;
        if (O && !c && (b == 17 || b == 18))return k;
        if (O && d && b == a)return k;
        switch (a) {case 13:return i;case 27:return!Q}
        return Yd(a)
    },Yd = function(a) {
        if (a >= 48 && a <= 57)return i;
        if (a >= 96 && a <= 106)return i;
        if (a >= 65 && a <= 90)return i;
        switch (a) {case 32:case 63:case 107:case 109:case 110:case 111:case 186:case 189:case 187:case 188:case 190:case 191:case 192:case 222:case 219:case 220:case 221:return i;default:return k}
    };
    var be = function(a) {
        a && $d(this, a)
    };
    M(be, ud);
    I = be[A];
    I.g = j;
    I.Ea = j;
    I.Va = j;
    I.Fa = j;
    I.ma = -1;
    I.S = -1;
    var ce = {"3":13,"12":144,"63232":38,"63233":40,"63234":37,"63235":39,"63236":112,"63237":113,"63238":114,"63239":115,"63240":116,"63241":117,"63242":118,"63243":119,"63244":120,"63245":121,"63246":122,"63247":123,"63248":44,"63272":46,"63273":36,"63275":35,"63276":33,"63277":34,"63289":144,"63302":45},de = {Up:38,Down:40,Left:37,Right:39,Enter:13,F1:112,F2:113,F3:114,F4:115,F5:116,F6:117,F7:118,F8:119,F9:120,F10:121,F11:122,F12:123,"U+007F":46,Home:36,End:35,PageUp:33,PageDown:34,Insert:45},ee = {61:187,
        59:186},fe = O || Q && R("525");
    be[A].Lb = function(a) {
        if (fe && !Zd(a[w], this.ma, a.shiftKey, a.ctrlKey, a.altKey))this[Ba](a);
        else this.S = P && a[w]in ee ? ee[a[w]] : a[w]
    };
    be[A].Mb = function() {
        this.S = this.ma = -1
    };
    be[A].handleEvent = function(a) {
        var b = a.ga,c,d;
        if (O && a[x] == "keypress") {
            c = this.S;
            d = c != 13 && c != 27 ? b[w] : 0
        }
        else if (Q && a[x] == "keypress") {
            c = this.S;
            d = b[xa] >= 0 && b[xa] < 63232 && Yd(c) ? b[xa] : 0
        }
        else if (dc) {
                c = this.S;
                d = Yd(c) ? b[w] : 0
            }
            else {
                c = b[w] || this.S;
                d = b[xa] || 0;
                if (gc && d == 63 && !c)c = 191
            }
        var e = c,f = b.keyIdentifier;
        if (c)if (c >= 63232 && c in ce)e = ce[c];
        else {
            if (c == 25 && a.shiftKey)e = 9
        }
        else if (f && f in de)e = de[f];
        a = e == this.ma;
        this.ma = e;
        b = new ge(e, d, a, b);
        try {
            this[ra](b)
        }
        finally {
            b.q()
        }
    };
    var $d = function(a, b) {
        a.Fa && a.detach();
        a.g = b;
        a.Ea = X(a.g, "keypress", a);
        a.Va = X(a.g, "keydown", a.Lb, k, a);
        a.Fa = X(a.g, "keyup", a.Mb, k, a)
    };
    be[A].detach = function() {
        if (this.Ea) {
            Y(this.Ea);
            Y(this.Va);
            Y(this.Fa);
            this.Fa = this.Va = this.Ea = j
        }
        this.g = j;
        this.ma = -1
    };
    be[A].d = function() {
        be.f.d[E](this);
        this.detach()
    };
    var ge = function(a, b, c, d) {
        d && this.H(d, m);
        ha(this, "key");
        ga(this, a);
        this.charCode = b;
        this.repeat = c
    };
    M(ge, Nc);
    var ie = function(a) {
        for (var b; a;) {
            b = fb(a);
            if (b = he[b])break;
            a = a.f ? a.f.constructor : j
        }
        if (b)return bb(b.Q) ? b.Q() : new b;
        return j
    },ke = function(a, b) {
        if (!a)g(l("Invalid class name " + a));
        if (!bb(b))g(l("Invalid decorator function " + b));
        je[a] = b
    },he = {},je = {};
    var le = function() {
    },me;
    Za(le);
    I = le[A];
    I.xa = function(a, b, c) {
        if (a = a.P ? a.P() : a)if (O && !R("7")) {
            var d = ne(this, tb(a), b);
            d[s](b);
            ib(c ? ub : vb, a)[G](j, d)
        }
        else c ? ub(a, b) : vb(a, b)
    };
    I.sb = function(a, b) {
        var c;
        if (a.A & 32 && (c = a.ya())) {
            if (!b && a.j & 32) {
                try {
                    c.blur()
                }
                catch(d) {
                }
                a.j & 32 && a.Kb(j)
            }
            a = c;
            var e = a.getAttributeNode("tabindex");
            if (e && e.specified) {
                a = a.tabIndex;
                a = typeof a == "number" && a >= 0
            }
            else a = k;
            a != b && Bc(c, b)
        }
    };
    I.K = function(a, b, c) {
        var d = a.P();
        if (d) {
            var e = this.Ra(b);
            e && this.xa(a, e, c);
            if (P) {
                me || (me = Db(1, "disabled", 4, "pressed", 8, "selected", 16, "checked", 64, "expanded"));
                (a = me[b]) && Xd(d, a, c)
            }
        }
    };
    I.ya = function(a) {
        return a.P()
    };
    I.w = function() {
        return"goog-control"
    };
    var ne = function(a, b, c) {
        var d = [];
        if (c)b = b[pa]([c]);
        lb([], function(e) {
            var f;
            a:{
                f = ib(mb, b);
                if (e[Sa])f = e[Sa](f, m);
                else if (q[Sa])f = q[Sa](e, f, m);
                else {
                    for (var h = e[z],o = L(e) ? e[C]("") : e,r = 0; r < h; r++)if (r in o && !f[E](m, o[r], r, e)) {
                        f = k;
                        break a
                    }
                    f = i
                }
            }
            if (f && (!c || mb(e, c)))d[s](e[Ua]("_"))
        });
        return d
    };
    le[A].Ra = function(a) {
        this.bb || oe(this);
        return this.bb[a]
    };
    var oe = function(a) {
        var b = a.w();
        a.bb = Db(1, b + "-disabled", 2, b + "-hover", 4, b + "-active", 8, b + "-selected", 16, b + "-checked", 32, b + "-focused", 64, b + "-open")
    };
    var Z = function(a, b, c) {
        Ud[E](this, c);
        this.I = b || ie(this.constructor);
        this.eb = a
    };
    M(Z, Ud);
    I = Z[A];
    I.eb = j;
    I.j = 0;
    I.A = 39;
    I.$a = 255;
    I.ac = 0;
    I.ec = i;
    I.s = j;
    I.ya = function() {
        return this.I.ya(this)
    };
    var pe = function(a, b) {
        if (b) {
            if (a.s)mb(a.s, b) || a.s[s](b);
            else a.s = [b];
            a.I.xa(a, b, i)
        }
    },qe = function(a, b) {
        if (b && a.s) {
            nb(a.s, b);
            if (a.s[z] == 0)a.s = j;
            a.I.xa(a, b, k)
        }
    };
    I = Z[A];
    I.xa = function(a, b) {
        b ? pe(this, a) : qe(this, a)
    };
    I.ha = function() {
        Z.f.ha[E](this);
        this.Da && this.Da.detach();
        this.jb() && this.Ua() && this.I.sb(this, k)
    };
    I.d = function() {
        Z.f.d[E](this);
        if (this.Da) {
            this.Da.q();
            delete this.Da
        }
        delete this.I;
        this.s = this.eb = j
    };
    I.jb = function() {
        return this.ec
    };
    I.Ua = function() {
        return!!!(this.j & 1)
    };
    I.Z = function(a) {
        var b;
        b = this.pa;
        b = !!b && typeof b.Ua == "function" && !b.Ua();
        if (!b && re(this, 1, !a)) {
            if (!a) {
                this.setActive(k);
                this.$b(k)
            }
            this.jb() && this.I.sb(this, a);
            this.K(1, !a)
        }
    };
    I.$b = function(a) {
        re(this, 2, a) && this.K(2, a)
    };
    I.setActive = function(a) {
        re(this, 4, a) && this.K(4, a)
    };
    var se = function(a, b) {
        re(a, 32, b) && a.K(32, b)
    };
    Z[A].K = function(a, b) {
        if (this.A & a && b != !!(this.j & a)) {
            this.I.K(this, a, b);
            this.j = b ? this.j | a : this.j & ~a
        }
    };
    var te = function(a, b, c) {
        if (a.Ca && a.j & b && !c)g(l("Component already rendered"));
        !c && a.j & b && a.K(b, k);
        a.A = c ? a.A | b : a.A & ~b
    },re = function(a, b, c) {
        return!!(a.A & b) && !!(a.j & b) != c && (!(a.ac & b) || a[ra](Vd(b, c))) && !a.Rb()
    };
    Z[A].Kb = function() {
        this.$a & 4 && this.A & 4 && this.setActive(k);
        this.$a & 32 && this.A & 32 && se(this, k)
    };
    if (!bb(Z))g(l("Invalid component class " + Z));
    if (!bb(le))g(l("Invalid renderer class " + le));
    var ue = fb(Z);
    he[ue] = le;
    ke("goog-control", function() {
        return new Z(j)
    });
    var ve = function() {
    };
    M(ve, le);
    Za(ve);
    ve[A].w = function() {
        return"goog-menuseparator"
    };
    var we = function(a, b) {
        Z[E](this, j, a || ve.Q(), b);
        te(this, 1, k);
        te(this, 2, k);
        te(this, 4, k);
        te(this, 32, k);
        this.j = 1
    };
    M(we, Z);
    ke("goog-menuseparator", function() {
        return new we
    });
    var xe = function() {
    };
    Za(xe);
    xe[A].ya = function(a) {
        return a.P()
    };
    xe[A].w = function() {
        return"goog-container"
    };
    var ye = function() {
        this.cb = []
    };
    M(ye, le);
    Za(ye);
    ye[A].Ra = function(a) {
        switch (a) {case 2:a = this.cb[0];if (!a) {
            switch (0) {case 0:a = this.w() + "-highlight";break;case 1:a = this.w() + "-checkbox";break;case 2:a = this.w() + "-content";break}
            this.cb[0] = a
        }return a = a;case 16:case 8:return"goog-option-selected";default:return ye.f.Ra[E](this, a)}
    };
    ye[A].w = function() {
        return"goog-menuitem"
    };
    var ze = function(a, b, c, d) {
        Z[E](this, a, d || ye.Q(), c);
        this.mb = b
    };
    M(ze, Z);
    ke("goog-menuitem", function() {
        return new ze(j)
    });
    var Ae = function() {
    };
    M(Ae, xe);
    Za(Ae);
    Ae[A].w = function() {
        return"goog-menu"
    };
    ke("goog-menuseparator", function() {
        return new we
    });
    var Be = /^(?:([^:\/?#]+):)?(?:\/\/(?:([^\/?#]*)@)?([^\/?#:@]*)(?::([0-9]+))?)?([^?#]+)?(?:\?([^#]*))?(?:#(.*))?$/,Ce = function(a) {
        return a.match(Be)
    };
    var Ee = function(a, b) {
        var c;
        if (a instanceof Ee) {
            this.$(b == j ? a.k : b);
            Fe(this, a.z);
            Ge(this, a.ta);
            He(this, a.ea);
            Ie(this, a.V);
            Je(this, a.qa);
            Ke(this, a.m.o());
            Le(this, a.ia)
        }
        else if (a && (c = Ce(n(a)))) {
            this.$(!!b);
            Fe(this, c[1] || "", i);
            Ge(this, c[2] || "", i);
            He(this, c[3] || "", i);
            Ie(this, c[4]);
            Je(this, c[5] || "", i);
            Ke(this, c[6] || "", i);
            Le(this, c[7] || "", i)
        }
        else {
            this.$(!!b);
            this.m = new Me(j, this, this.k)
        }
    };
    I = Ee[A];
    I.z = "";
    I.ta = "";
    I.ea = "";
    I.V = j;
    I.qa = "";
    I.ia = "";
    I.Sb = k;
    I.k = k;
    I.toString = function() {
        if (this.h)return this.h;
        var a = [];
        this.z && a[s](Ne(this.z, Oe), ":");
        if (this.ea) {
            a[s]("//");
            this.ta && a[s](Ne(this.ta, Oe), "@");
            a[s](Pe(this.ea));
            this.V != j && a[s](":", n(this.V))
        }
        this.qa && a[s](Ne(this.qa, Qe));
        var b = n(this.m);
        b && a[s]("?", b);
        this.ia && a[s]("#", Ne(this.ia, Re));
        return this.h = a[Ua]("")
    };
    I.o = function() {
        var a;
        a = this.z;
        var b = this.ta,c = this.ea,d = this.V,e = this.qa,f = this.m.o(),h = this.ia,o = new Ee(j, this.k);
        a && Fe(o, a);
        b && Ge(o, b);
        c && He(o, c);
        d && Ie(o, d);
        e && Je(o, e);
        f && Ke(o, f);
        h && Le(o, h);
        return a = o
    };
    var Fe = function(a, b, c) {
        Se(a);
        delete a.h;
        a.z = c ? b ? ea(b) : "" : b;
        if (a.z)a.z = a.z[u](/:$/, "");
        return a
    },Ge = function(a, b, c) {
        Se(a);
        delete a.h;
        a.ta = c ? b ? ea(b) : "" : b;
        return a
    },He = function(a, b, c) {
        Se(a);
        delete a.h;
        a.ea = c ? b ? ea(b) : "" : b;
        return a
    },Ie = function(a, b) {
        Se(a);
        delete a.h;
        if (b) {
            b = Number(b);
            if (isNaN(b) || b < 0)g(l("Bad port number " + b));
            a.V = b
        }
        else a.V = j;
        return a
    },Je = function(a, b, c) {
        Se(a);
        delete a.h;
        a.qa = c ? b ? ea(b) : "" : b;
        return a
    },Ke = function(a, b, c) {
        Se(a);
        delete a.h;
        if (b instanceof Me) {
            a.m = b;
            a.m.sa = a;
            a.m.$(a.k)
        }
        else {
            c ||
            (b = Ne(b, Te));
            a.m = new Me(b, a, a.k)
        }
        return a
    },Xe = function(a, b, c) {
        Se(a);
        delete a.h;
        K(c) || (c = [n(c)]);
        var d = a.m;
        b = b;
        c = c;
        Ue(d);
        Ve(d);
        b = We(d, b);
        if (d.u(b)) {
            var e = d.c.v(b);
            if (K(e))d.a -= e[z];
            else d.a--
        }
        if (c[z] > 0) {
            d.c.J(b, c);
            d.a += c[z]
        }
        return a
    },Le = function(a, b, c) {
        Se(a);
        delete a.h;
        a.ia = c ? b ? ea(b) : "" : b;
        return a
    },Se = function(a) {
        if (a.Sb)g(l("Tried to modify a read-only Uri"))
    };
    Ee[A].$ = function(a) {
        this.k = a;
        this.m && this.m.$(a)
    };
    var Pe = function(a) {
        if (L(a))return aa(a);
        return j
    },Ye = /^[a-zA-Z0-9\-_.!~*'():\/;?]*$/,Ne = function(a, b) {
        var c = j;
        if (L(a)) {
            c = a;
            Ye.test(c) || (c = encodeURI(a));
            if (c.search(b) >= 0)c = c[u](b, Ze)
        }
        return c
    },Ze = function(a) {
        a = a.charCodeAt(0);
        return"%" + (a >> 4 & 15)[y](16) + (a & 15)[y](16)
    },Oe = /[#\/\?@]/g,Qe = /[\#\?]/g,Te = /[\#\?@]/g,Re = /#/g,Me = function(a, b, c) {
        this.r = a || j;
        this.sa = b || j;
        this.k = !!c
    },Ue = function(a) {
        if (!a.c) {
            a.c = new Ic;
            if (a.r)for (var b = a.r[C]("&"),c = 0; c < b[z]; c++) {
                var d = b[c][v]("="),e = j,f = j;
                if (d >= 0) {
                    e = b[c][Ra](0,
                            d);
                    f = b[c][Ra](d + 1)
                }
                else e = b[c];
                e = Ib(e);
                e = We(a, e);
                a.add(e, f ? Ib(f) : "")
            }
        }
    };
    I = Me[A];
    I.c = j;
    I.a = j;
    I.add = function(a, b) {
        Ue(this);
        Ve(this);
        a = We(this, a);
        if (this.u(a)) {
            var c = this.c.v(a);
            K(c) ? c[s](b) : this.c.J(a, [c,b])
        }
        else this.c.J(a, b);
        this.a++;
        return this
    };
    I.remove = function(a) {
        Ue(this);
        a = We(this, a);
        if (this.c.u(a)) {
            Ve(this);
            var b = this.c.v(a);
            if (K(b))this.a -= b[z];
            else this.a--;
            return this.c.remove(a)
        }
        return k
    };
    I.u = function(a) {
        Ue(this);
        a = We(this, a);
        return this.c.u(a)
    };
    I.R = function() {
        Ue(this);
        for (var a = this.c.G(),b = this.c.R(),c = [],d = 0; d < b[z]; d++) {
            var e = a[d];
            if (K(e))for (var f = 0; f < e[z]; f++)c[s](b[d]);
            else c[s](b[d])
        }
        return c
    };
    I.G = function(a) {
        Ue(this);
        if (a) {
            a = We(this, a);
            if (this.u(a)) {
                var b = this.c.v(a);
                if (K(b))return b;
                else {
                    a = [];
                    a[s](b)
                }
            }
            else a = []
        }
        else {
            b = this.c.G();
            a = [];
            for (var c = 0; c < b[z]; c++) {
                var d = b[c];
                K(d) ? pb(a, d) : a[s](d)
            }
        }
        return a
    };
    I.J = function(a, b) {
        Ue(this);
        Ve(this);
        a = We(this, a);
        if (this.u(a)) {
            var c = this.c.v(a);
            if (K(c))this.a -= c[z];
            else this.a--
        }
        this.c.J(a, b);
        this.a++;
        return this
    };
    I.v = function(a, b) {
        Ue(this);
        a = We(this, a);
        if (this.u(a)) {
            a = this.c.v(a);
            return K(a) ? a[0] : a
        }
        else return b
    };
    I.toString = function() {
        if (this.r)return this.r;
        if (!this.c)return"";
        for (var a = [],b = 0,c = this.c.R(),d = 0; d < c[z]; d++) {
            var e = c[d],f = Hb(e);
            e = this.c.v(e);
            if (K(e))for (var h = 0; h < e[z]; h++) {
                b > 0 && a[s]("&");
                a[s](f, "=", Hb(e[h]));
                b++
            }
            else {
                b > 0 && a[s]("&");
                a[s](f, "=", Hb(e));
                b++
            }
        }
        return this.r = a[Ua]("")
    };
    var Ve = function(a) {
        delete a.Oa;
        delete a.r;
        a.sa && delete a.sa.h
    };
    Me[A].o = function() {
        var a = new Me;
        if (this.Oa)a.Oa = this.Oa;
        if (this.r)a.r = this.r;
        if (this.c)a.c = this.c.o();
        return a
    };
    var We = function(a, b) {
        b = n(b);
        if (a.k)b = b[Va]();
        return b
    };
    Me[A].$ = function(a) {
        if (a && !this.k) {
            Ue(this);
            Ve(this);
            Gc(this.c, function(b, c) {
                var d = c[Va]();
                if (c != d) {
                    this.remove(c);
                    this.add(d, b)
                }
            }, this)
        }
        this.k = a
    };
    var $e = function(a, b) {
        this.sa = new Ee(a);
        this.Ab = b ? b : "callback";
        this.Za = 5000
    },af = 0;
    $e[A].send = function(a, b, c, d) {
        if (!p[Fa][za]) {
            c && c(a);
            return j
        }
        d = d || "_" + (af++)[y](36) + jb()[y](36);
        J._callbacks_ || (J._callbacks_ = {});
        var e = p[ya]("script"),f = j;
        if (this.Za > 0) {
            f = bf(d, e, a, c);
            f = J[Ia](f, this.Za)
        }
        c = this.sa.o();
        for (var h in a)if (!a[La] || a[La](h))Xe(c, h, a[h]);
        if (b) {
            a = cf(d, e, b, f);
            J._callbacks_[d] = a;
            Xe(c, this.Ab, "_callbacks_." + d)
        }
        pc(e, {type:"text/javascript",id:d,charset:"UTF-8",src:c[y]()});
        p.getElementsByTagName("head")[0][ma](e);
        return{Ta:d,Za:f}
    };
    var bf = function(a, b, c, d) {
        return function() {
            df(a, b, k);
            d && d(c)
        }
    },cf = function(a, b, c, d) {
        return function() {
            J.clearTimeout(d);
            df(a, b, i);
            c[G](m, arguments)
        }
    },df = function(a, b, c) {
        J[Ia](function() {
            wc(b)
        }, 0);
        if (J._callbacks_[a])if (c)delete J._callbacks_[a];
        else J._callbacks_[a] = Ya
    };
    var ef = k;
    Wa("ae.init", function() {
        ff();
        gf();
        hf();
        ld(ba, "load", function() {
            ef = i
        })
    }, m);
    var ff = function() {
        var a = S("ae-content");
        if (a) {
            a = nc("table", "ae-table-striped", a);
            for (var b = 0,c; c = a[b]; b++) {
                c = nc("tbody", j, c);
                for (var d = 0,e; e = c[d]; d++) {
                    e = nc("tr", j, e);
                    for (var f = 0,h; h = e[f]; f++)f % 2 && ub(h, "ae-even")
                }
            }
        }
    },hf = function() {
        var a = nc(j, "ae-noscript");
        lb(ob(a), function(b) {
            vb(b, "ae-noscript")
        })
    },gf = function() {
        var a = S("ae-content");
        if (a) {
            a = nc("form", j, a);
            for (var b = 0,c; c = a[b]; b++) {
                c = nc("a", "ae-cancel", c);
                for (var d = 0,e; e = c[d]; d++) {
                    var f = e[Qa],h = uc("input", {type:"button",className:"ae-cancel",value:"Cancel"});
                    X(h, "click", function() {
                        la(ba[Ka], f)
                    });
                    e[H] && e[H].insertBefore(h, e);
                    wc(e)
                }
            }
        }
    },jf = j,kf = "UA-3739047-3";
    Wa("ae.initAnalytics", function(a) {
        jf = a;
        lf()
    }, m);
    var lf = function() {
        if (jf) {
            var a = jf._getTracker(kf);
            a._initData();
            a._trackPageview()
        }
    };
    Wa("ae.trackPageView", lf, m);
    var $ = {};
    kf = "UA-3739047-5";
    $.xb = ["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"];
    $.ua = [];
    $.B = j;
    $.N = j;
    $.M = j;
    $.ba = [];
    $.aa = "-link";
    $.H = function() {
        $.ab = S("ae-trust-detail-chart-title");
        $.ca = p[ya]("div");
        $.ca.id = "ae-trust-detail-chart-disclaimer";
        var a = $.ab;
        a[H] && a[H].insertBefore($.ca, a.nextSibling);
        $.Bb = S("ae-trust-detail-chart-description");
        $.Na = S("ae-trust-detail-chart-img");
        $.wa = [];
        $.ub = S("ae-trust-detail-today");
        $.ub && $.wa[s]($.ub);
        $.ob = S("ae-trust-detail-newer");
        $.ob && $.wa[s]($.ob);
        $.pb = S("ae-trust-detail-older");
        $.pb && $.wa[s]($.pb);
        $.Cb = S("ae-trust-detail-chart-img-c");
        X($.Na, "load", $.Eb);
        $.bc = S("ae-trust-detail-table");
        $.Ib();
        $.history = new Cd;
        X($[Na], "navigate", $.Ob);
        $[Na].Z(i);
        (a = (a = Ad($[Na])) ? S(a + $.aa) : $.ua[0]) && $.Xa(a)
    };
    Wa("ae.Trust.Detail.init", $.H, m);
    $.Ob = function(a) {
        a = a.dc;
        $.B && $.B.id[u]($.aa, "") != a && $.Xa(S(a + $.aa))
    };
    $.Ib = function() {
        $.ua = nc("a", j, S("ae-trust-detail-nav"));
        for (var a = 0,b; b = $.ua[a]; a++) {
            var c = b[H].getElementsByTagName("input");
            if (c && c[z]) {
                b.da = Lc(c[0]);
                b.gb = Lc(c[1]);
                b.description = Lc(c[2]);
                X(b, "click", $.Gb);
                b.kb = [];
                for (var d = 3,e; e = c[d]; d++) {
                    var f = Lc(e)[C](",");
                    if (f[z] == 2) {
                        e = f[0];
                        f = da(f[1], 10)
                    }
                    else f = e = j;
                    b.kb[s]({lineName:e,startIndex:f})
                }
                b.Ia = []
            }
        }
    };
    $.Jb = function(a) {
        for (var b = a[na](a[v]("/", 7)),c = 0,d; d = $.ua[c]; c++)if (d.da && (d.da == a || d.da == b))return d
    };
    $.Gb = function(a) {
        var b = a[ta];
        $.Xa(b);
        a.preventDefault()
    };
    $.Xa = function(a) {
        if ($.B != a) {
            $.B && vb($.B, "ae-active");
            $.B = a;
            ub(a, "ae-active");
            lf();
            if ($.Na) {
                $.Na.src = a.da;
                var b = "",c = "";
                if (a.title)b = a.title;
                if (a.description)c = a.description;
                Ac($.ab, b);
                if (a.gb) {
                    $.ca.innerHTML = a.gb;
                    Rd($.ca, i)
                }
                else Rd($.ca, k);
                Ac($.Bb, c);
                b = a.id[u]($.aa, "");
                c = $[Na];
                if (Ad(c) != b)if (c.L) {
                    zd(c, b, k);
                    if (!yd) {
                        O && Bd(c, b, k, m);
                        c.fa && c.qb()
                    }
                }
                else {
                    Bd(c, b, k);
                    c.U = c.na = c.ja.value = b;
                    c[ra](new Ed(b))
                }
                b = /\#.*/;
                c = 0;
                for (var d; d = $.wa[c]; c++) {
                    la(d, d[Qa][u](b, ""));
                    la(d, d[Qa] + "#" + a.id[u]($.aa, ""))
                }
            }
        }
    };
    $.Eb = function(a) {
        a = a[ta].src;
        var b = $.Jb(a);
        if ($.ba[z])for (var c = 0,d; d = $.ba[c]; c++)Rd(d, k);
        $.db();
        $.ba = [];
        if (b.Ia && b.Ia[z])for (c = 0; d = b.Ia[c]; c++) {
            Rd(d, i);
            $.ba[s](d)
        }
        else b.da.charAt(0) != "/" && (new $e(a + "&chof=json")).send({}, hb($.Db, $, b))
    };
    $.Db = function(a, b) {
        if (b && b.chartshape) {
            b = $.Tb(b);
            for (var c = 0,d; d = a.kb[c]; c++) {
                var e = d.lineName;
                d = d.startIndex;
                for (var f in b)if (e && e == f) {
                    var h = b[e];
                    e = h[d];
                    d = h[d + 1] - 14;
                    h = uc("div", {className:"ae-trust-marker"}, $.xb[c]);
                    h.rowIndex = c;
                    X(h, "click", $.Fb);
                    var o = m,r = m,t = P && (gc || hc) && R("1.9");
                    if (e instanceof wb) {
                        o = e.x;
                        r = e.y
                    }
                    else {
                        o = e;
                        r = d
                    }
                    h[Ma].left = typeof o == "number" ? (t ? Math.round(o) : o) + "px" : o;
                    h[Ma].top = typeof r == "number" ? (t ? Math.round(r) : r) + "px" : r;
                    $.Cb[ma](h);
                    a.Ia[s](h);
                    a == $.B ? $.ba[s](h) : Rd(h, k);
                    break
                }
            }
        }
    };
    $.vb = function(a) {
        for (var b = [],c = -1,d = 0; d < a[z]; d += 2) {
            var e = a[d],f = a[d + 1];
            if (e < c)break;
            b[s](e);
            b[s](f);
            c = e
        }
        return b
    };
    $.Tb = function(a) {
        for (var b = {},c = 0,d; d = a.chartshape[c]; c++)if (b[d[Da]])for (var e = $.vb(d.coords),f = 0,h; h = e[f]; f++)b[d[Da]][s](h);
        else if (d[Da][Ra](0, 4) == "line")b[d[Da]] = $.vb(d.coords);
        return b
    };
    $.Fb = function(a) {
        $.db();
        $.M = a[Pa];
        ub($.M, "ae-active");
        $.N = $.bc.rows[$.M.rowIndex + 1];
        ub($.N, "ae-active");
        var b = $.N,c,d = lc(b),e = Pd(b, "position"),f = P && d[wa] && !b.getBoundingClientRect && e == "absolute" && (c = d[wa](b)) && (c[ua] < 0 || c[va] < 0);
        a = new wb(0, 0);
        var h;
        c = d ? d[oa] == 9 ? d : lc(d) : p;
        h = O && !Cc(mc(c)) ? c[D] : c[Fa];
        if (b != h)if (b.getBoundingClientRect) {
            c = b.getBoundingClientRect();
            if (O) {
                b = b.ownerDocument;
                c.left -= b[Fa].clientLeft + b[D].clientLeft;
                c.top -= b[Fa].clientTop + b[D].clientTop
            }
            c = c;
            d = mc(d);
            d = !Q && d.C.compatMode == "CSS1Compat" ?
                d.C[Fa] : d.C[D];
            d = new wb(d.scrollLeft, d.scrollTop);
            a.x = c.left + d.x;
            a.y = c.top + d.y
        }
        else if (d[wa] && !f) {
                c = d[wa](b);
                d = d[wa](h);
                a.x = c[ua] - d[ua];
                a.y = c[va] - d[va]
            }
            else {
                c = b;
                do{
                    a.x += c.offsetLeft;
                    a.y += c.offsetTop;
                    if (c != b) {
                        a.x += c.clientLeft || 0;
                        a.y += c.clientTop || 0
                    }
                    if (Q && Pd(c, "position") == "fixed") {
                        a.x += d[D].scrollLeft;
                        a.y += d[D].scrollTop;
                        break
                    }
                    c = c.offsetParent
                } while (c && c != b);
                if (dc || Q && e == "absolute")a.y -= d[D].offsetTop;
                for (c = b; (c = Qd(c)) && c != d[D] && c != h;) {
                    a.x -= c.scrollLeft;
                    if (!dc || c.tagName != "TR")a.y -= c.scrollTop
                }
            }
        a =
        a;
        a = a.y;
        ba.scrollTo(0, a)
    };
    $.db = function() {
        if ($.N) {
            vb($.N, "ae-active");
            $.N = j
        }
        if ($.M) {
            vb($.M, "ae-active");
            $.M = j
        }
    };
})();
