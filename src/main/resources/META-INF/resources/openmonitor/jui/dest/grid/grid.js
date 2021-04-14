(function(){JUI.Class("Grid",{OPTS:$.extend({url:null,method:"GET",idField:null,data:null,width:"100%",pageList:[10,20,30,40,50],loadFilter:null,pageNumber:1,pageSize:10,striped:true,hover:true,bordered:true,checkTdWidth:50,checkboxDeepTrigger:false,pagination:false,rownumbers:false,rownumberTitle:"序号",rowNumberTdWidth:50,sortName:null,sortOrder:"ASC",emptyDataMsg:'<div style="text-align:center;">无数据</div>',pageSelectStyle:"float:left;margin-right:10px;height:34px;border: 1px solid #ddd;",singleSelect:false,checkOnSelect:true,queryParams:{},selectCache:false,initLoad:true,clickHighlight:true,autoFit:false,loader:null},JUIConfig.Grid),init:function(a){this._super(a)},afterRender:function(){this.page=new JUI.Page(this);if(this.opt("initLoad")){this.load()}},resetSelectCache:function(){this.selectCache={}},getSelectCache:function(){if(!this.selectCache){this.selectCache={}}return this.selectCache},isSelectCache:function(){return this.opt("selectCache")},isInCache:function(a){if(!a){return false}return !!this.selectCache[a]},hasData:function(){var a=this.data&&this.data[this.opt("serverRowsName")]&&this.data[this.opt("serverRowsName")].length>0;return a},load:function(b){this.resetSelectCache();if(this.opt("url")){this.schData=b||{};this.schData=$.extend(this.schData,this.opt("queryParams"));this.goPage(1)}else{var a=this.opt("data");this.loadData(a)}},goPage:function(a){a=parseInt(a)||1;this.page.goPage(a)},reload:function(){var e=this;var f=this.buildSchData();var c=this.fire("BeforeLoad",{data:f});if(c===false){return}var b=this.opt("url");var a=this.opt("loader");var d={url:b,method:this.opt("method"),data:f,dataType:"json"};if(a){a(d,function(g){e.bindResult(g)})}else{if(b){this.requestServer(d)}else{this.requestLocal(this.page.pageIndex,this.page.pageSize)}}},requestServer:function(a){var b=this;$.ajax({type:a.method,url:a.url,traditional:true,dataType:a.dataType,data:a.data,success:function(d,e,c){b.bindResult(d)},error:function(c,e,d){b.fire("LoadError",{xhr:c,status:e,error:d})}})},requestLocal:function(a,c){var f=this;var d=[];var g=this.localData[this.opt("serverRowsName")];var b=1,e=0;if(f.opt("pagination")&&g.length>0){b=parseInt((a-1)*c);e=g.length;d=g.slice(b,b+c)}else{d=g}var h={};h[f.opt("serverRowsName")]=d;h[f.opt("serverPageIndexName")]=a;h[f.opt("serverPageSizeName")]=c;h[f.opt("serverTotalName")]=e;f.bindResult(h)},removeData:function(){this.bindResult({})},loadData:function(a){a=a||{};this.opt("url","");this.resetSelectCache();this.localData=this.formatData(a);this.goPage(1)},bindResult:function(a){a=this.formatData(a);this.fire("LoadSuccess",{data:a});this.data=this.filterData(a);this.page.setPageInfo(this.data);this.view.refresh()},setPageSize:function(a){a=parseInt(a)||this.opt("pageSize");this.page.setPageSize(a)},getData:function(){return this.data},getRows:function(){return this.data[this.opt("serverRowsName")]},getChecked:function(){var d=[];if(this.isSelectCache()){var b=this.getSelectCache();for(var a in b){if(b[a]){d.push(b[a])}}}var f=this.view.getCheckedIndex();for(var e=0,h=f.length;e<h;e++){var g=f[e];var k=this.getRowByIndex(g);var j=k[this.opt("idField")];var c=!this.isInCache(j);if(c){d.push(k)}}return d},getCheckIds:function(){var e=this.getChecked();var c=[];var b=this.opt("idField");for(var d=0,a=e.length;d<a;d++){var f=e[d];c.push(f[b])}return c},getSelected:function(){var a=this.getChecked();if(a.length==0){return null}return a[0]},getRowByIndex:function(a){var b=this.getRows();return b[a]},checkAll:function(){this.view.checkAll()},uncheckAll:function(){this.view.uncheckAll()},clearChecked:function(){this.uncheckAll()},unselectAll:function(){this.uncheckAll()},clearSelections:function(){this.uncheckAll()},selectRow:function(a){this.view.selectRow(a)},unselectRow:function(a){this.view.unselectRow(a)},checkRow:function(a){this.view.checkRow(a)},uncheckRow:function(a){this.view.uncheckRow(a)},selectRecord:function(a){this.view.selectRecord(a)},isCheckedAll:function(){return this.view.isCheckedAll()},sort:function(b,a){this.opt("sortName",b);this.opt("sortOrder",a);this.fire("SortColumn",{sortName:b,sortOrder:a});this.goPage(1)},getViewClass:function(){return JUI.GridView},buildSchData:function(){var b=this.schData||{};var a=this.page.getPageData();b=$.extend(b,a);if(this.opt("sortName")){b[this.opt("requestSortName")]=this.opt("sortName");b[this.opt("requestOrderName")]=this.opt("sortOrder")}return b},getTableWidth:function(){var a=this.opt("width");if(this.opt("autoFit")){a="auto"}return a},formatData:function(a){a=a||{};if($.isArray(a)){var b={};b[this.opt("serverRowsName")]=a;a=b}if(!a[this.opt("serverRowsName")]){a[this.opt("serverRowsName")]=[]}return a},filterData:function(a){var b=this.opt("loadFilter");if($.isFunction(b)){a=b(a)}return a}},JUI.Component)})();