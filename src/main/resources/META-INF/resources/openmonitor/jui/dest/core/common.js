(function(){JUI.Class("Common",{OPTS:{},init:function(a){this.opts=$.extend({},this.OPTS,a)},getDefOpts:function(){return this.OPTS},opt:function(b,a){if(b&&typeof b==="string"){if(a!==undefined){this.opts[b]=a}else{return this.opts[b]}}},on:function(a,b){this.opt("on"+a,b)},set:function(a,b){this.opt(a,b)},getOpts:function(){return this.opts},options:function(){return this.getOpts()},fire:function(d){var b=this.opts["on"+d];if(b){var c=[];for(var e=1,a=arguments.length;e<a;e++){c.push(arguments[e])}return b.apply(this,c)}},runOptFun:function(e){var c=this.opts[e];if(c){var b=[];for(var d=1,a=arguments.length;d<a;d++){b.push(arguments[d])}return c.apply(this,b)}}})})();