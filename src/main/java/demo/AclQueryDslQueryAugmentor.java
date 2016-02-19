package demo;

import java.math.BigInteger;

import org.springframework.data.jpa.repository.support.QueryDslJpaQueryContext;
import org.springframework.data.jpa.repository.support.QueryDslJpaUpdateContext;
import org.springframework.data.jpa.repository.support.QueryDslQueryContext;
import org.springframework.data.repository.augment.AnnotationBasedQueryAugmentor;
import org.springframework.data.repository.augment.QueryContext.QueryMode;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

/**
 * @author gazal
 */
public class AclQueryDslQueryAugmentor extends
		AnnotationBasedQueryAugmentor<demo.Acled, QueryDslJpaQueryContext<?>, QueryDslQueryContext, QueryDslJpaUpdateContext<?>> {

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AnnotationBasedQueryAugmentor#prepareQuery(org.springframework.data.repository.augment.QueryContext, java.lang.annotation.Annotation)
	 */
	@Override
	protected QueryDslJpaQueryContext<?> prepareQuery(QueryDslJpaQueryContext<?> context, Acled annotation) {

		augmentPermission(context);
		return context;
	}

	/**
	 * Augment permission.
	 *
	 * @param context the context
	 * @return the query dsl jpa query context
	 */
	private static QueryDslJpaQueryContext<?> augmentPermission(QueryDslJpaQueryContext<?> context) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			return context;
		}

		Predicate hasPermission = Expressions.numberTemplate(Integer.class, "function('BITAND', {0}, {1})",
				QAclEntry.aclEntry.mask, Expressions.constant(getRequiredPermission(context.getMode())))
				.goe(Expressions.constant(1));

		Predicate matchingDomainType = QAclEntry.aclEntry.objectIdentity.aclClass.class_
				.eq(context.getRoot().getType().getName());

		Predicate matchingDomainId = QAclEntry.aclEntry.objectIdentity.objectIdIdentity
				.eq(Expressions.numberPath(BigInteger.class, context.getRoot(), "id"));

		Predicate matchingUser = QAclEntry.aclEntry.sid.sid.eq(authentication.getName());

		context.getQuery().from(context.getRoot(), QAclEntry.aclEntry).where(hasPermission, matchingDomainId,
				matchingDomainType, matchingUser);
		return context;
	}

	/**
	 * Gets the required permission.
	 *
	 * @param mode the mode
	 * @return the required permission
	 */
	private static int getRequiredPermission(QueryMode mode) {

		switch (mode) {
			case FOR_DELETE:
			case FOR_UPDATE:
				return BasePermission.WRITE.getMask();
			default:
				return BasePermission.READ.getMask();
		}
	}

}
