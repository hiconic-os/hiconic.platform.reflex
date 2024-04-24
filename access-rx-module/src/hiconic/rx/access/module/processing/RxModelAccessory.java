package hiconic.rx.access.module.processing;

import com.braintribe.model.generic.reflection.ConfigurableCloningContext;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.session.api.managed.ManagedGmSession;
import com.braintribe.model.processing.session.api.managed.ModelAccessory;
import com.braintribe.model.processing.session.impl.managed.BasicManagedGmSession;
import com.braintribe.utils.lcd.LazyInitialized;

public class RxModelAccessory implements ModelAccessory {
	private CmdResolver cmdResolver;
	private LazyInitialized<ManagedGmSession> lazySession = new LazyInitialized<ManagedGmSession>(this::newSession);
	
	public RxModelAccessory(CmdResolver cmdResolver) {
		this.cmdResolver = cmdResolver;
	}

	@Override
	public CmdResolver getCmdResolver() {
		return cmdResolver;
	}
	
	@Override
	public GmMetaModel getModel() {
		return cmdResolver.getModelOracle().getGmMetaModel();
	}
	
	@Override
	public ManagedGmSession getModelSession() {
		return lazySession.get();
	}
	
	private ManagedGmSession newSession() {
		BasicManagedGmSession session = new BasicManagedGmSession();
		GmMetaModel model = getModel();


		session.setMetaModel(GmMetaModel.T.getModel().getMetaModel());
		
		ConfigurableCloningContext cloningContext = ConfigurableCloningContext.build().supplyRawCloneWith(session).done();
		model.clone(cloningContext);

		session.setModelAccessory(this);
		
		return session;
	}
	
	@Override
	public ModelOracle getOracle() {
		return cmdResolver.getModelOracle();
	}

}
