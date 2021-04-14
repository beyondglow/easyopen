String.prototype.startWith=function(str){
	if(!str) {return false;}
	return this.substring(0,str.length) == str;
}  
String.prototype.endWith=function(str){    
	if(!str) {return false;}
	return this.substring(this.length - str.length) == str;       
}

function initTree() {
	var _$tree = $('#tree').ztree_toc({
        _header_nodes: [{
            id: 1,
            pId: 0,
            name: document.title,
            open: false
        }],
        // 第一个节点
        ztreeSetting: {
            view: {
                dblClickExpand: false,
                showLine: true,
                showIcon: false,
                selectedMulti: false
            },
            data: {
                simpleData: {
                    enable: true,
                    idKey: "id",
                    pIdKey: "pId"
                    // rootPId: "0"
                }
            },
            callback: {
                beforeClick: function(treeId, treeNode) {
                    $('a').removeClass('curSelectedNode');
                    if (treeNode.id == 1) {
                        $('body').scrollTop(0);
                    }
                    if ($.fn.ztree_toc.defaults.is_highlight_selected_line == true) {
                        $('#' + treeNode.id).css('color', 'red').fadeOut("slow",
                        function() {
                            $(this).show().css('color', 'black');
                        });
                    }
                }
            }
        },
        is_auto_number: false,
        // 菜单是否显示编号，如果markdown标题没有数字标号可以设为true
        documment_selector: '.markdown-body',
        is_expand_all: expandAll == 'true' // 菜单全部展开
    });

    var _$cont = $('#readme');
    var _$treeOpt = $('.tree-opt');
    $('#hideTree').click(function () {
        var $a = $(this);
        if (_$tree.is(':visible')) {
            $a.html('显示');
            $('#tree').hide();
            _$treeOpt.hide();
            _$cont.css({'margin-left': '0%'});
        } else {
            $a.html('隐藏');
            _$tree.show();
            _$treeOpt.show();
            _$cont.css({'margin-left': '25%'});

        }
    });

    var tree = $.fn.zTree.getZTreeObj('tree');
    $('#treeExp').click(function () {
        tree.expandAll(true);
    });

    $('#treeCol').click(function () {
        tree.expandAll(false);
    });
}

function initEvent() {
	// 代码高亮
    $('.highlight').each(function(i, block) {
        hljs.highlightBlock(block);
    });
    
    // 请求按钮
    $('.api-block').find('.post-btn').click(function() {
        var $self = $(this);
        var _parent = $(this).parents('.api-block');

        request($self, _parent, function (resp,postDataStr) {
            var _param = _parent.find('.api-data');
            var _result = _parent.find('.api-result');
            var _postResult = _parent.find('.post-result');
            _param.val(postDataStr);
            _result.val(formatJson(JSON.stringify(resp)));
            _postResult.show();
        }, function (e) {
            console.error(e);
            alert('请求出错，按F12查看console');
        })

        return false;
    });


    $('.api-block').find('.label-param').click(function () {
        var $self = $(this);
        var _parent = $self.parents('.api-block');
        $self.addClass('title-bold').siblings().removeClass('title-bold');
        _parent.find('.busi-param').show();
        _parent.find('.busi-param-model').hide();
        _parent.attr('show-param', true);
    });

    $('.api-block').find('.label-param-model').click(function () {
        var $self = $(this);
        var _parent = $self.parents('.api-block');
        $self.addClass('title-bold').siblings().removeClass('title-bold');
        _parent.find('.busi-param').hide();
        _parent.find('.busi-param-model').show();
        _parent.attr('show-param', false);
    });

    $('.api-block').find('.label-param-urldecode').click(function () {
        var $self = $(this);
        var _parent = $self.parents('.api-block');
        var $txt = _parent.find('.busi-param-model-txt').eq(0);
        var json = $txt.val();
        $txt.val(formatJson(decodeURIComponent(json)));
    });

    $('.api-block').find('.label-result').click(function () {
        var $self = $(this);
        var _parent = $self.parents('.api-block');
        $self.addClass('title-bold').siblings().removeClass('title-bold');
        _parent.find('.result-table').show();
        _parent.find('.busi-result-model').hide();
    });

    $('.api-block').find('.label-result-model').click(function () {
        var $self = $(this);
        var _parent = $self.parents('.api-block');
        $self.addClass('title-bold').siblings().removeClass('title-bold');
        _parent.find('.result-table').hide();
        _parent.find('.busi-result-model').show();
    });

    $('.api-item').find('.new-win-btn').click(function(){
    	var $div = $(this).parents('.api-item');
    	
    	window.open("../opendoc/new.html?id=" + $div.attr('id'))
    	
    	return false;
    });

    var inputId = 0;
    
    $('.api-item').find('.add-array-btn').click(function(){
    	var $btn = $(this);
    	var $td = $btn.parent();
        var $input = $td.children('input').eq(0);
        var $table = $td.children('table').eq(0);
        if ($input.length > 0) {
            var $newInput = $input.clone();
            if($newInput.attr('type') == 'file') {
                $newInput.attr('name','upload_file_' + inputId++)
            }
            $newInput.css({'display':'table', 'margin-top':'1px'}).attr('id',$input.attr('id') + inputId++).val('');
            $td.append($newInput);
        } else if ($table.length > 0) {
            var $tableNew = $table.clone();
            $td.append($tableNew);
        }
    });
    
    $('.api-item').find('.del-array-btn').click(function(){
    	var $btn = $(this);
    	var $td = $btn.parent();
    	var $inputs = $td.children('input');
    	var $tables = $td.children('table');
        if ($inputs.length > 0) {
            var inputCount = $inputs.length;
            if(inputCount > 1) {
                $inputs.eq(inputCount - 1).remove();
            }
        } else if ($tables.length > 0) {
            var tableCount = $tables.length;
            if(tableCount > 1) {
                $tables.eq(tableCount - 1).remove();
            }
        }

    });
}


function getFormData($form) {
    var data = {};
    var $table = $form.find('> table').eq(0);

    buildData(data, $table);

    formatData(data)

    return data;
}

function buildData(data, $table) {
    // 查询table下面的一级tr
    var $trs = $table.find('> tbody > tr');

    $trs.each(function (i, tr) {
        var $tr = $(tr);
        var $typeTd = $tr.find('.param-type').eq(0);
        var isArray = 'array' == $typeTd.html();
        // 查找tr下面的一级td，不包括嵌套td
        var $tds = $tr.find('>td');
        $tds.each(function(i, el){
            var $td = $(el);
            var $childTdTables = $td.children('table')
            if($childTdTables.length > 0) {
                $childTdTables.each(function(i, childTdTable){
                    var $childTdTable = $(childTdTable)
                    var _parentname = $childTdTable.attr('parentname');
                    var parentData = data[_parentname]
                    // 如果数据已经存在
                    if (parentData) {
                        // 如果是数组
                        if($.isArray(parentData)) {
                            var item = {}
                            parentData.push(item)
                            buildData(item, $childTdTable);
                        } else {
                            var _data = {};
                            data[_parentname] = isArray ? [_data] : _data;
                            buildData(_data, $childTdTable);
                        }
                    } else {
                        var _data = {};
                        data[_parentname] = isArray ? [_data] : _data;
                        buildData(_data, $childTdTable);
                    }
                })
            } else {
                var $els = $td.children('input[type="text"]');
                if($els.length > 0) {
                    $els.each(function(i, el){
                        var name = el.name;
                        var elVal = el.value;
                        if (elVal) {
                            var dataValue = data[name];

                            if (dataValue) {
                                if ($.isArray(dataValue)) {
                                    elVal && dataValue.push(elVal);
                                } else {
                                    data[name] = [dataValue, elVal];
                                }
                            } else {
                                data[name] = $(el).attr('arrinput') ? [elVal] : elVal;
                            }
                        }
                    });
                }
            }
        });

    });
}

/**
 * 判断是否为空的对象
 * @param obj
 * @returns {boolean}
 */
function isEmptyObject(obj) {
    if (!isObject(obj)) {
        return false;
    }
    for(var key in obj) {
        // 有kay说明对象不为空
        return false;
    }
    return true;
}

function isObject(obj) {
    if (!obj) {
        return false;
    }
    return Object.prototype.toString.call(obj) === '[object Object]';
}

function isArray(obj) {
    if (!obj) {
        return false;
    }
    return Object.prototype.toString.call(obj) === '[object Array]';
}

function formatData(data) {
    for (var key in data) {
        var obj = data[key]
        if (isArray(obj)) {
            for (var i = 0; i < obj.length; i++) {
                formatData(obj[i])
            }
            for (var i = 0; i < obj.length; i++) {
                if(isEmptyObject(obj[i])) {
                    obj.splice(i, 1);
                    i = -1
                }
            }
        } else if (isObject(obj)) {
            if (isEmptyObject(obj)) {
                delete data[key]
            } else {
                formatData(obj)
                if (isEmptyObject(obj)) {
                    delete data[key]
                }
            }
        }
    }
}

function request($self, _parent, success, error) {
    var btnTxt = $self.html();
    var errorHanlder = function (e) {
        enablePostButton($self, btnTxt);
        return error(e);
    }
    try {
        var $paramModelTextArea = _parent.find('.busi-param-model-txt').eq(0);
        var name = _parent.find('.api-name').val();
        var version = _parent.find('.api-version').val();

        var $form = _parent.find('form').eq(0);
        var hasFile = $form.find('input[type="file"]').length > 0;

        var jsonData;
        if ($paramModelTextArea.is(':visible')) {
            jsonData = JSON.parse($paramModelTextArea.val() || '{}');
        } else {
            jsonData = getFormData($form);
        }
        console.log(jsonData)

        disablePostButton($self);
        sdk.config({
            url: $('#url').val(),
            app_key: $('#appKey').val(),
            secret: $('#secret').val(),
        });
        sdk.post({
            name: name // 接口名
            ,version:version
            ,access_token:$('#access_token').val()
            ,jwt:$('#jwt').val()
            ,data: jsonData // 请求参数
            ,form:hasFile ? $form.get(0) : null
            ,callback: function(resp, postDataStr) { // 成功回调
                enablePostButton($self, btnTxt);
                success(resp, postDataStr);
            }
            ,error:function (jsonData, paramStr, xhr) {
                errorHanlder(xhr);
            }
        });
    }catch (e) {
        errorHanlder(e);
    }
}

/**
 * 禁用请求按钮
 * @param $self button标签jquery对象
 */
function disablePostButton($self) {
    $self.html('请求中...').prop('disabled', 'disable').addClass('btn-disabled');
}

/**
 * 启用请求按钮
 * @param $self button标签jquery对象
 * @param btnTxt 按钮文本
 */
function enablePostButton($self, btnTxt) {
    $self.removeClass('btn-disabled').html(btnTxt).prop('disabled', '');
}