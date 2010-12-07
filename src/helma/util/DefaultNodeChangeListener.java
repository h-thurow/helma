package helma.util;

import helma.framework.core.Application;
import helma.framework.core.RequestEvaluator;
import helma.objectmodel.db.Node;
import helma.scripting.ScriptingEngine;
import helma.scripting.ScriptingException;

import java.util.Iterator;
import java.util.List;

public class DefaultNodeChangeListener implements
		helma.objectmodel.db.NodeChangeListener {

	Application _application;
	
	public DefaultNodeChangeListener(Application application) {
		_application = application;
	}
	
	@SuppressWarnings("unchecked")
	public void nodesChanged(List inserted, List updated, List deleted,
			List parents) {
            RequestEvaluator reval = _application.getCurrentRequestEvaluator();
            if (reval != null) {
            	Iterator nodes = inserted.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke((Node) nodes.next(), "onInserted", RequestEvaluator.EMPTY_ARGS, ScriptingEngine.ARGS_WRAP_DEFAULT, false);
					} catch (ScriptingException e) {
						_application.logError("Error invoking onInserted().", e);
					}
            	}
            	
            	nodes = updated.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke((Node) nodes.next(), "onUpdated", RequestEvaluator.EMPTY_ARGS, ScriptingEngine.ARGS_WRAP_DEFAULT, false);
					} catch (ScriptingException e) {
						_application.logError("Error invoking onUpdated().", e);
					}
            	}
            	
            	nodes = deleted.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke((Node) nodes.next(), "onDeleted", RequestEvaluator.EMPTY_ARGS, ScriptingEngine.ARGS_WRAP_DEFAULT, false);
					} catch (ScriptingException e) {
						_application.logError("Error invoking onDeleted().", e);
					}
            	}
            	
            	nodes = parents.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke((Node) nodes.next(), "onChanged", RequestEvaluator.EMPTY_ARGS, ScriptingEngine.ARGS_WRAP_DEFAULT, false);
					} catch (ScriptingException e) {
						_application.logError("Error invoking onChanged().", e);
					}
            	}
            }
	}

}
