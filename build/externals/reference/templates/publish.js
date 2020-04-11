require("app/JsHilite.js");

function publish(fileGroup, context) {
    var file_template = new JsPlate(context.t+"main.tmpl");
    
    // loop through the files
    for (var i = 0; i < fileGroup.files.length; i++) {
    
        // loop through the symbols
        for (var s = 0; s < fileGroup.files[i].symbols.length; s++) {
            var symbol = fileGroup.files[i].symbols[s];
            
            // look for symbols without memberof property
            if (!symbol.memberof) {
            
                // look for symbols with a prefix in their name
                if (symbol.name.indexOf('.') > -1) {
                    
                    // setting memberof property to the current symbol's prefix
                    symbol.memberof = symbol.name.slice(0,symbol.name.indexOf('.'));
                    
                    // stripping prefix from the current symbol's name property
                    symbol.name = symbol.name.slice(symbol.name.indexOf('.')+1);
                }
            }
            
            // collect and massage see tag information for easier output
            symbol.renderedSeeTags = [];
            var seeitems = symbol.doc.tags;
            for (var seeitem in seeitems) {
                if (seeitems[seeitem].title == 'see') {
                    var itemName = seeitems[seeitem].desc;
                    var memberOf = symbol.memberof;
                    var itemUrl = itemName;
                    
                    // references pointing into the helma or jala namespaces
                    if (itemName.split('.')[0] == 'helma' || itemName.split('.')[0] == 'jala') 
                        itemUrl = itemName.split('.')[0] +'.'+ itemName.split('.')[1] +'.html#'+ itemName;
                    
                    // references that are relative to the parent object
                    else if (!itemName.indexOf('#')) 
                        itemUrl = (memberOf||'global') +'.html#'+ (memberOf && memberOf +'.')+ itemName.slice(1);
                    
                    // references that do not contain a period but are not relative to the parent object 
                    else if (itemName.split('.').length == 1) 
                        itemUrl = itemName +'.html';
                    
                    // other references that are absolut and contain one period
                    else if (itemName.split('.').length == 2) 
                        itemUrl = itemName.split('.')[0]+'.html#'+ 
                            (itemName.split('.')[0]=='global' ? itemName.split('.')[1] : itemName);
                    
                    // references that point into a specific prototype
                    else if (itemName.split('.prototype.').length == 2) 
                        // this currently assumes the reference is poitning to a core
                        // module extending a built-in prototype 
                        itemUrl = itemName.split('.prototype.')[0]+'Extensions.html#'+ 
                            (itemName.split('.prototype.')[0] +'.'+ itemName.split('.prototype.')[1]);
                    
                    // references pointing into the Packages.helma namespace
                    else if (!itemName.indexOf('Packages.helma.'))
                        itemUrl = 'http://adele.helma.org/download/helma/apidocs/api/'+
                            itemName.split('.').slice(1).join('/') +'.html';
                    
                    // other references pointing into the Packages namespace
                    else if (!itemName.indexOf('Packages.'))
                        itemUrl = 'http://google.com/search?q='+ itemName.slice(9);
                    
                    var itemToSee = '<a href="'+ itemUrl +'">'+ ((!itemName.indexOf('#')) ? 
                        (memberOf && memberOf +'.')+  itemName.slice(1) : itemName) +'</a>';
                    
                    symbol.renderedSeeTags.push('<div class="see">'+itemToSee+'</div>');
                }
            }
        }
    }
    
    var index = {};
    if (context.d) {
        for (var i = 0; i < fileGroup.files.length; i++) {
            var group = {name:"global"};
            for (var s = 0; s < fileGroup.files[i].symbols.length; s++) {
                var symbol = fileGroup.files[i].symbols[s];
                group.name =  symbol.memberof +'.'+ symbol.name;
                if (symbol.memberof) {
                    group.memberof = symbol.memberof;
                }
                
                // group non-global objects two levels deep
                if (group.memberof){
                    var groupArray = group.memberof.split('.');
                    if (groupArray.length > 2)
                        group.name = groupArray[0]+'.'+ groupArray[1];
                    else
                        group.name = group.memberof;
                }
                
                // 
                var groupFilename = group.name +'.html';
                if (fileGroup.files[i].path.indexOf('/core/') > -1) {
                    // check for macro filters
                    if (group.name.indexOf('_filter') > -1)
                        groupFilename = 'MacroFilters.html';
                    // check for global macros
                    else if (group.name.indexOf('_macro') > -1)
                        groupFilename = 'GlobalMacros.html';
                    // give the JSON module special treatment as well
                    else if (fileGroup.files[i].path.indexOf('JSON.js') > -1)
                        groupFilename = 'JSON.html';
                    // otherwise assume it extends a built-in prototype
                    else
                        groupFilename = group.name +'Extensions.html';
                }
                if (fileGroup.files[i].path.indexOf('/jala/code/') > -1) {
                    if (fileGroup.files[i].path.indexOf('HopObject.js') > -1)
                        groupFilename = 'jala.HopObjectExtensions.html';
                }
                    
                index[groupFilename] = {name: (group.name), file: fileGroup.files[i]};
            }
        }
        
        for (var i in index){
            if (index[i].file) {
                var output = file_template.process(index[i].file);
                IO.saveFile(context.d, i, output);
            }
        }
        
        // add docjar page
        var output = file_template.process({docjar:true});
        IO.saveFile(context.d, 'javalibs.html', output);
    }
    
    if (context.d) {        
        IO.saveFile(context.d,"index.html",IO.readFile(context.d+"global.html"));
        IO.copyFile(context.t+"styles.css", context.d);
        IO.copyFile(context.t+"scripts.js", context.d);
        
        IO.copyFile(context.t+"helmaheader.gif", context.d);
        IO.copyFile(context.t+"file.gif", context.d);
        IO.copyFile(context.t+"overview.gif", context.d);
        IO.copyFile(context.t+"constructor.gif", context.d);
        IO.copyFile(context.t+"function.gif", context.d);
        IO.copyFile(context.t+"object.gif", context.d);
    }
}

