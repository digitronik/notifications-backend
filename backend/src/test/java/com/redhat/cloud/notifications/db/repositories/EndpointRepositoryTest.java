package com.redhat.cloud.notifications.db.repositories;

import com.redhat.cloud.notifications.config.FeatureFlipper;
import com.redhat.cloud.notifications.models.CompositeEndpointType;
import com.redhat.cloud.notifications.models.Endpoint;
import com.redhat.cloud.notifications.models.EndpointType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.persistence.TypedQuery;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
public class EndpointRepositoryTest {

    @Inject
    FeatureFlipper featureFlipper;

    @Test
    void queryBuilderTest_AccountId() {
        featureFlipper.setUseOrgId(false);
        queryBuilderTest(false, "accountId");
    }

    @Test
    void queryBuilderTest_OrgId() {
        featureFlipper.setUseOrgId(true);
        queryBuilderTest(true, "orgId");
        featureFlipper.setUseOrgId(false);
    }

    public void queryBuilderTest(boolean useOrgId, String accountIdOrOrgId) {
        TypedQuery<Endpoint> query = mock(TypedQuery.class);

        // types with subtype and without it
        EndpointRepository.queryBuilderEndpointsPerType(
                null,
                null,
                useOrgId,
                null,
                Set.of(
                        new CompositeEndpointType(EndpointType.WEBHOOK),
                        new CompositeEndpointType(EndpointType.CAMEL, "splunk")
                ),
                null
        ).build((hql, endpointClass) -> {
            assertEquals("SELECT e FROM Endpoint e WHERE e." + accountIdOrOrgId + " IS NULL AND (e.compositeType.type IN (:endpointType) OR e.compositeType IN (:compositeTypes))", hql);
            return query;
        });

        verify(query, times(2)).setParameter((String) any(), any());
        verifyNoMoreInteractions(query);
        clearInvocations(query);

        // without sub-types
        EndpointRepository.queryBuilderEndpointsPerType(
                null,
                null,
                useOrgId,
                null,
                Set.of(
                        new CompositeEndpointType(EndpointType.WEBHOOK)
                ),
                null
        ).build((hql, endpointClass) -> {
            assertEquals("SELECT e FROM Endpoint e WHERE e." + accountIdOrOrgId + " IS NULL AND (e.compositeType.type IN (:endpointType))", hql);
            return query;
        });

        verify(query, times(1)).setParameter((String) any(), any());
        verifyNoMoreInteractions(query);
        clearInvocations(query);

        // with sub-types
        EndpointRepository.queryBuilderEndpointsPerType(
                null,
                null,
                useOrgId,
                null,
                Set.of(
                        new CompositeEndpointType(EndpointType.CAMEL, "splunk")
                ),
                null
        ).build((hql, endpointClass) -> {
            assertEquals("SELECT e FROM Endpoint e WHERE e." + accountIdOrOrgId + " IS NULL AND (e.compositeType IN (:compositeTypes))", hql);
            return query;
        });

        verify(query, times(1)).setParameter((String) any(), any());
        verifyNoMoreInteractions(query);
        clearInvocations(query);
    }

}
