/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

var fs = require('fs');
var http = require("http");

function parseParams(body) {
	var lines = body.split('\r\n');
	var paramName = null;
	var paramValue = "";
	var params = {};
	for(var i = 0; i < lines.length; i++) {
		if (lines[i].startsWith("--------------------------")) {
			if (paramName != null) {
				params[paramName] = paramValue;
				paramName = null;
				paramValue = "";
			}
			continue;
		}
		if (lines[i].startsWith("Content-Disposition:")) {
			paramName = lines[i].split('=')[1];
			paramName = paramName.substring(1, paramName.length - 1);
			i++;
			continue;
		}
		if (lines[i] === "") {
			continue;
		}
		paramValue += lines[i];
	}
	
	return params;
}

function evalJS(code, echo) {
	//console.log("Executing JS code: '" + code + "'");
	var res = {}; 
	try {
		r = eval(code)
		res.isError = false;
		if (echo) {
			res.data = JSON.stringify(r);
		} else {
			res.data = JSON.stringify(null);
		}
	} catch (error) {
		//console.log("Caught error:" + JSON.stringify(error));
		res.isError = true;
		res.data = "" + error;
	}
	return res;
}

var rHandlerScript = fs.readFileSync( __dirname + "/handler.fr", "utf8");
Interop.eval("application/x-r", rHandlerScript);

rParser = Interop.import('parser');
rResult = Interop.import('result');
rIsError = Interop.import('isError');
deparseObject = Interop.import('deparseObject');

function evalR(code, echo) {
	rParser(code);
	if (echo) {
		Interop.eval("application/x-r", "err <- TRUE; out <- tryCatch({ err <- TRUE; r <- eval(exp); err <- FALSE; r }, error = function(e) e$message)");
	} else {
		Interop.eval("application/x-r", "err <- TRUE; out <- tryCatch({ err <- TRUE; eval(exp); err <- FALSE; NULL }, error = function(e) e$message)");
	}
	var res = {}
	res.data = rResult();
	res.isError = rIsError();
	return res;
}

var rubyHandlerScript = fs.readFileSync( __dirname + "/handler.rb", "utf8");
Interop.eval("application/x-ruby", rubyHandlerScript);
rubyStoreExpr = Interop.import('storeExpr');
rubyToJSON = Interop.import('rubyToJSON');

function evalRuby(code, echo) {
	rubyStoreExpr(code);
	var r;
	code = `begin
		eval($expr)
	rescue Exception => exc
		-1
	end`;
	if (echo) {
		r = Interop.eval("application/x-ruby", code);
	} else {
		r = Interop.eval("application/x-ruby", code);
	}
	var res = {}
	rJSON = rubyToJSON(r);
	//console.log("Result from Ruby:" + rJSON);
	res.data = rJSON;
	res.isError = false;
	return res;
}

function processPost(request, response) {
    var queryData = "";

    if(request.method == 'POST') {
        request.on('data', function(data) {
            queryData += data;
            if(queryData.length > 1e6) {
                queryData = "";
                response.writeHead(413, {'Content-Type': 'text/plain'}).end();
                request.connection.destroy();
            }
        });

        request.on('end', function() {
        	var params = parseParams(queryData);
			//console.log("Params: " + JSON.stringify(params));
        	var echo = params.echo === "TRUE";
        	var res;
        	if (params.mimetype == "application/x-r") {
	        	res = evalR(params.code, echo);
			} else if (params.mimetype == "text/javascript") {
	        	res = evalJS(params.code, echo);
			} else if (params.mimetype == "application/x-ruby") {
	        	res = evalRuby(params.code, echo);
	      	} else {
        		res = {
        			isError : true,
        			data : deparseObject("Unsupported language: " + params.mimetype)
        		}
        	}
			if (res.isError) {
				response.writeHead(400, {'Content-Type': 'text/plain'});
			} else {
				response.writeHead(200, "OK", {'Content-Type': 'text/plain'});
			}
			response.end(res.data);

        });

    } else {
        response.writeHead(405, {'Content-Type': 'text/plain'});
        response.end();
    }
}

// Launch the server
console.log("Starting GraalVM agent...");

var server = http.createServer(function (inp, out) {
	var command = inp.url.substring(1);
	if (command == "ping") {
		out.end("pong");
	} else if (command == "stop") {
		console.log("GraalVM agent stopped");
		out.end(command, null, function() {
			process.exit(0);
		});
	} else {
		if(inp.method == 'POST') {
			processPost(inp, out);
		} else {
			out.writeHead(200, "OK", {'Content-Type': 'text/plain'});
			out.end();
		}
	}
});

host = process.argv[2]
port = parseInt(process.argv[3])
server.listen(port, host);
server.on('error', function(err) {
    console.log("Caught error: " + err);
});

console.log("GraalVM agent accepting at " + host + ":" + port);
