package com.redhat.cloud.notifications.db.repositories;

import com.redhat.cloud.notifications.OrgIdHelper;
import com.redhat.cloud.notifications.models.EmailSubscription;
import com.redhat.cloud.notifications.models.EmailSubscriptionType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class EmailSubscriptionRepository {

    @Inject
    EntityManager entityManager;

    @Inject
    OrgIdHelper orgIdHelper;

    @Transactional
    public boolean subscribe(String accountNumber, String orgId, String username, String bundleName, String applicationName, EmailSubscriptionType subscriptionType) {
        // TODO NOTIF-603 After the migration, change the ON CONFLICT clause to:
        // ON CONFLICT (account_id, org_id, user_id, application_id, subscription_type) DO NOTHING
        String query = "INSERT INTO endpoint_email_subscriptions(account_id, org_id, user_id, application_id, subscription_type) " +
                "SELECT :accountId, :orgId, :userId, a.id, :subscriptionType " +
                "FROM applications a, bundles b WHERE a.bundle_id = b.id AND a.name = :applicationName AND b.name = :bundleName " +
                "ON CONFLICT (account_id, user_id, application_id, subscription_type) DO UPDATE SET org_id = :orgId";
        // HQL does not support the ON CONFLICT clause so we need a native query here
        entityManager.createNativeQuery(query)
                .setParameter("accountId", accountNumber)
                .setParameter("orgId", orgId)
                .setParameter("userId", username)
                .setParameter("bundleName", bundleName)
                .setParameter("applicationName", applicationName)
                .setParameter("subscriptionType", subscriptionType.name())
                .executeUpdate();
        return true;
    }

    @Transactional
    public boolean unsubscribe(String accountNumber, String orgId, String username, String bundleName, String applicationName, EmailSubscriptionType subscriptionType) {
        if (orgIdHelper.useOrgId(orgId)) {
            String query = "DELETE FROM EmailSubscription WHERE orgId = :orgId AND id.userId = :userId " +
                    "AND id.applicationId = (SELECT a.id FROM Application a, Bundle b WHERE a.bundle.id = b.id " +
                    "AND b.name = :bundleName AND a.name = :applicationName) AND id.subscriptionType = :subscriptionType";
            entityManager.createQuery(query)
                    .setParameter("orgId", orgId)
                    .setParameter("userId", username)
                    .setParameter("bundleName", bundleName)
                    .setParameter("applicationName", applicationName)
                    .setParameter("subscriptionType", subscriptionType)
                    .executeUpdate();
        } else {
            String query = "DELETE FROM EmailSubscription WHERE id.accountId = :accountId AND id.userId = :userId " +
                    "AND id.applicationId = (SELECT a.id FROM Application a, Bundle b WHERE a.bundle.id = b.id " +
                    "AND b.name = :bundleName AND a.name = :applicationName) AND id.subscriptionType = :subscriptionType";
            entityManager.createQuery(query)
                    .setParameter("accountId", accountNumber)
                    .setParameter("userId", username)
                    .setParameter("bundleName", bundleName)
                    .setParameter("applicationName", applicationName)
                    .setParameter("subscriptionType", subscriptionType)
                    .executeUpdate();
        }
        return true;
    }

    public EmailSubscription getEmailSubscription(String accountNumber, String orgId, String username, String bundleName, String applicationName, EmailSubscriptionType subscriptionType) {
        if (orgIdHelper.useOrgId(orgId)) {
            String query = "SELECT es FROM EmailSubscription es LEFT JOIN FETCH es.application a LEFT JOIN FETCH a.bundle b " +
                    "WHERE es.orgId = :orgId AND es.id.userId = :userId " +
                    "AND b.name = :bundleName AND a.name = :applicationName AND es.id.subscriptionType = :subscriptionType";
            try {
                return entityManager.createQuery(query, EmailSubscription.class)
                        .setParameter("orgId", orgId)
                        .setParameter("userId", username)
                        .setParameter("bundleName", bundleName)
                        .setParameter("applicationName", applicationName)
                        .setParameter("subscriptionType", subscriptionType)
                        .getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        } else {
            String query = "SELECT es FROM EmailSubscription es LEFT JOIN FETCH es.application a LEFT JOIN FETCH a.bundle b " +
                    "WHERE es.id.accountId = :accountId AND es.id.userId = :userId " +
                    "AND b.name = :bundleName AND a.name = :applicationName AND es.id.subscriptionType = :subscriptionType";
            try {
                return entityManager.createQuery(query, EmailSubscription.class)
                        .setParameter("accountId", accountNumber)
                        .setParameter("userId", username)
                        .setParameter("bundleName", bundleName)
                        .setParameter("applicationName", applicationName)
                        .setParameter("subscriptionType", subscriptionType)
                        .getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        }
    }

    public List<EmailSubscription> getEmailSubscriptionsForUser(String accountNumber, String orgId, String username) {
        if (orgIdHelper.useOrgId(orgId)) {
            String query = "SELECT es FROM EmailSubscription es LEFT JOIN FETCH es.application a LEFT JOIN FETCH a.bundle b " +
                    "WHERE es.orgId = :orgId AND es.id.userId = :userId";
            return entityManager.createQuery(query, EmailSubscription.class)
                    .setParameter("orgId", orgId)
                    .setParameter("userId", username)
                    .getResultList();
        } else {
            String query = "SELECT es FROM EmailSubscription es LEFT JOIN FETCH es.application a LEFT JOIN FETCH a.bundle b " +
                    "WHERE es.id.accountId = :accountId AND es.id.userId = :userId";
            return entityManager.createQuery(query, EmailSubscription.class)
                    .setParameter("accountId", accountNumber)
                    .setParameter("userId", username)
                    .getResultList();
        }
    }
}
