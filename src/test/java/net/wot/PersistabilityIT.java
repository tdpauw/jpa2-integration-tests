package net.wot;

import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.apache.commons.beanutils.PropertyUtils;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.junit.Test;

public class PersistabilityIT {

	final List<? extends Builder<?>> persistentObjectBuilders = Arrays.asList(CompanyBuilder.aCompany());
	final EntityManager entityManager = Persistence.createEntityManagerFactory("integration").createEntityManager();
	final Transactor transactor = new JPATransactor(entityManager);
	
	@Test
	public void roundTripPersistentObjects() throws Exception {
		for (Builder<?> builder : persistentObjectBuilders) {
			assertCanBePersisted(builder);
		}
	}

	private void assertCanBePersisted(Builder<?> builder) throws Exception {
		try {
			assertReloadsWithSameStateAs(persistedObjectFromBuilder(builder));
		} catch (PersistenceException e) {
			throw new PersistenceException("could not round-trip " + typeNameFor(builder), e);
		}
	}

	private Object persistedObjectFromBuilder(final Builder<?> builder) throws Exception {
		return transactor.performQuery(new QueryUnitOfWork() {
			public Object query() throws Exception {
				Object original = builder.build();
				entityManager.persist(original);
				return original;
			}
		});
	}

	
	private void assertReloadsWithSameStateAs(final Object original) throws Exception {
		transactor.perform(new UnitOfWork() {
			
			@Override
			public void work() throws Exception {
				assertThat(entityManager.find(original.getClass(), idOf(original)), SamePropertyValuesAs.samePropertyValuesAs(original));
			}
		});
	}
	
	protected Object idOf(Object original) throws Exception {
		return PropertyUtils.getProperty(original, "id");
	}

	private String typeNameFor(Builder<?> builder) {
		return builder.getClass().getSimpleName().replace("Builder", "");
	}
}