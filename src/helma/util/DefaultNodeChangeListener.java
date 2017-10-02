package helma.util;

import helma.framework.core.Application;
import helma.framework.core.RequestEvaluator;
import helma.scripting.ScriptingEngineInterface;
import helma.scripting.ScriptingException;

import java.util.Iterator;
import java.util.List;

public class DefaultNodeChangeListener implements
		helma.objectmodel.db.NodeChangeListenerInterface {

	Application _application;
	
	public DefaultNodeChangeListener(Application application) {
		this._application = application;
	}
	
	@SuppressWarnings("unchecked")
	public void nodesChanged(List inserted, List updated, List deleted,
			List parents) {
            RequestEvaluator reval = this._application.getCurrentRequestEvaluator();
            if (reval != null) {
            	Iterator nodes = inserted.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke(nodes.next(), "onInserted", RequestEvaluator.EMPTY_ARGS, ScriptingEngineInterface.ARGS_WRAP_DEFAULT, false); //$NON-NLS-1$
					} catch (ScriptingException e) {
						this._application.logError(Messages.getString("DefaultNodeChangeListener.0"), e); //$NON-NLS-1$
					}
            	}
            	
            	nodes = updated.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke(nodes.next(), "onUpdated", RequestEvaluator.EMPTY_ARGS, ScriptingEngineInterface.ARGS_WRAP_DEFAULT, false); //$NON-NLS-1$
					} catch (ScriptingException e) {
						this._application.logError(Messages.getString("DefaultNodeChangeListener.1"), e); //$NON-NLS-1$
					}
            	}
            	
            	nodes = deleted.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke(nodes.next(), "onDeleted", RequestEvaluator.EMPTY_ARGS, ScriptingEngineInterface.ARGS_WRAP_DEFAULT, false); //$NON-NLS-1$
					} catch (ScriptingException e) {
						this._application.logError(Messages.getString("DefaultNodeChangeListener.2"), e); //$NON-NLS-1$
					}
            	}
            	
            	nodes = parents.iterator();
            	while (nodes.hasNext()) {
            		try {
						reval.getScriptingEngine().invoke(nodes.next(), "onChanged", RequestEvaluator.EMPTY_ARGS, ScriptingEngineInterface.ARGS_WRAP_DEFAULT, false); //$NON-NLS-1$
					} catch (ScriptingException e) {
						this._application.logError(Messages.getString("DefaultNodeChangeListener.3"), e); //$NON-NLS-1$
					}
            	}
            }
	}

}
