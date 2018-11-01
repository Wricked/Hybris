package de.hybris.platform.servicelayer.cronjob.impl;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.catalog.model.CatalogModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.catalog.model.SyncItemJobModel;
import de.hybris.platform.catalog.model.synchronization.CatalogVersionSyncJobModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.BatchJobModel;
import de.hybris.platform.cronjob.model.CompositeJobModel;
import de.hybris.platform.cronjob.model.CronJobHistoryModel;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.servicelayer.cronjob.CronJobHistoryDao;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;



@IntegrationTest
public class DefaultCronJobHistoryDaoTest extends AbstractCronJobHistoryTest
{
	@Resource
	private ModelService modelService;
	@Resource
	private UserService userService;
	@Resource
	private CronJobHistoryDao cronJobHistoryDao;

	private List<CronJobHistoryModel> cronJobHistoryModelList = new ArrayList<>();
	private JobModel jobOne;
	private JobModel jobTwo;
	private UserModel adminUser;
	private UserModel anonymousUser;
	private Date baseStartDate;
	private Date baseFinishDate;

	@Before
	public void setUp()
	{

		adminUser = userService.getAdminUser();
		anonymousUser = userService.getAnonymousUser();
		baseStartDate = createDate(2016, 2, 11, 1, 0, 0);
		baseFinishDate = createDate(2016, 2, 11, 12, 3, 30);
		jobOne = modelService.create(CompositeJobModel.class);
		jobOne.setCode(String.format("%s_%s", "jobOne", RandomStringUtils.randomAlphanumeric(3)));
		jobTwo = modelService.create(CompositeJobModel.class);
		jobTwo.setCode(String.format("%s_%s", "jobTwo", RandomStringUtils.randomAlphanumeric(3)));

		cronJobHistoryModelList
				.add(createEntity(jobOne, adminUser, baseStartDate, baseFinishDate, CronJobResult.SUCCESS, CronJobStatus.FINISHED));
		cronJobHistoryModelList.add(createEntity(jobOne, anonymousUser, DateUtils.setMinutes(baseStartDate, 10),
				DateUtils.setMinutes(baseFinishDate, 14), CronJobResult.SUCCESS, CronJobStatus.FINISHED));
		cronJobHistoryModelList.add(createEntity(jobTwo, anonymousUser, DateUtils.setMinutes(baseStartDate, 3),
				DateUtils.setMinutes(baseFinishDate, 15), CronJobResult.SUCCESS, CronJobStatus.FINISHED));
		cronJobHistoryModelList.add(createEntity(jobTwo, adminUser, DateUtils.setSeconds(baseStartDate, 1),
				DateUtils.setSeconds(baseFinishDate, 59), CronJobResult.ERROR, CronJobStatus.FINISHED));
		cronJobHistoryModelList.add(createEntity(jobTwo, adminUser, DateUtils.setHours(baseStartDate, 13),
				DateUtils.setHours(baseFinishDate, 14), CronJobResult.ERROR, CronJobStatus.FINISHED));

	}

	@Test
	public void testFindCronJobHistoryByCronJobCode()
	{
		// given
		final CronJobHistoryModel expected = cronJobHistoryModelList.iterator().next();
		// when
		CronJobHistoryModel result = cronJobHistoryDao.findCronJobHistoryBy(expected.getCronJobCode()).iterator().next();
		// then
		Assertions.assertThat(result).isEqualTo(expected);
	}

	@Test
	public void testFindAllCronJobHistoryByJob()
	{
		// given
		final CronJobHistoryModel expected1 = cronJobHistoryModelList.get(0);
		final CronJobHistoryModel expected2 = cronJobHistoryModelList.get(1);

		// when
		List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(null, jobOne.getCode());

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(2);
		Assertions.assertThat(result).contains(expected1, expected2);
	}


	@Test
	public void testFindAllCronJobHistoryByUserAndJob()
	{
		// given
		final CronJobHistoryModel expected1 = cronJobHistoryModelList.get(0);

		// when
		List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(adminUser.getUid(), jobOne.getCode());

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(1);
		Assertions.assertThat(result).contains(expected1);
	}

	@Test
	public void testFindAllCronJobHistoryByUserAndCronJobStatus()
	{
		// given
		final CronJobStatus expectedStatus = CronJobStatus.FINISHED;

		// when
		List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(null, null, null, null, expectedStatus);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(5);
		Assertions.assertThat(result).containsAll(cronJobHistoryModelList);


		// when
		result = cronJobHistoryDao.findCronJobHistoryBy(anonymousUser.getUid(), null, null, null, expectedStatus);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(2);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(1), cronJobHistoryModelList.get(2));
	}

	@Test
	public void testFindAllCronJobHistoryByUserAndStartDateAndCronJobStatus()
	{
		// given
		final CronJobStatus expectedStatus = CronJobStatus.FINISHED;
		final Date startDate = DateUtils.setMinutes(baseStartDate, 10);


		// when
		List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(null, null, startDate, null, expectedStatus);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(2);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(1), cronJobHistoryModelList.get(4));

		// when
		result = cronJobHistoryDao.findCronJobHistoryBy(anonymousUser.getUid(), null, startDate, null, expectedStatus);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(1);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(1));
	}



	@Test
	public void testFindAllCronJobHistoryByCronJobResult()
	{
		// given
		final CronJobResult cronJobResult = CronJobResult.SUCCESS;

		// when
		List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(null, null, null, null, cronJobResult);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(3);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(0), cronJobHistoryModelList.get(1),
				cronJobHistoryModelList.get(2));

		result = cronJobHistoryDao.findCronJobHistoryBy(adminUser.getUid(), null, null, null, cronJobResult);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(1);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(0));
	}


	@Test
	public void testFindAllCronJobHistoryByUser()
	{
		// given
		final UserModel givenUser = anonymousUser;

		// when
		final List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(givenUser.getUid(), null, null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(2);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(1), cronJobHistoryModelList.get(2));
	}


	@Test
	public void testFindAllCronJobHistoryByUserAndJobItemType()
	{
		// given
		final UserModel givenUser = anonymousUser;

		// when
		final List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(givenUser.getUid(), jobTwo.getItemtype(),
				null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(2);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(1), cronJobHistoryModelList.get(2));
	}

	@Test
	public void testFindAllCronJobHistoryByUserAndJobItemTypeAndStartDate()
	{
		// given
		final Date startDate = DateUtils.setMinutes(baseStartDate, 15);

		// when
		List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(adminUser.getUid(), jobTwo.getItemtype(),
				startDate, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(1);
		Assertions.assertThat(result).contains(cronJobHistoryModelList.get(4));

	}


	@Test
	public void testFindAllCronJobHistoryByJobItemType()
	{
		// when
		List<CronJobHistoryModel> result = cronJobHistoryDao.findCronJobHistoryBy(null, jobTwo.getItemtype(), null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(5);
		Assertions.assertThat(result).containsAll(getCronJobHistoryModelList());


		// when
		result = cronJobHistoryDao.findCronJobHistoryBy(null, BatchJobModel._TYPECODE, null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).isEmpty();

		// given
		final CatalogModel catalog = modelService.create(CatalogModel.class);
		catalog.setId(RandomStringUtils.randomAlphanumeric(5));

		final CatalogVersionModel source = modelService.create(CatalogVersionModel.class);
		source.setVersion("staged");
		source.setCatalog(catalog);
		final CatalogVersionModel target = modelService.create(CatalogVersionModel.class);
		target.setVersion("online");
		target.setCatalog(catalog);

		final CatalogVersionSyncJobModel catalogSyncItemJob = modelService.create(CatalogVersionSyncJobModel.class);
		catalogSyncItemJob.setCode(String.format("%s_%s", "catalog_sync_job", RandomStringUtils.randomAlphanumeric(3)));
		catalogSyncItemJob.setSourceVersion(source);
		catalogSyncItemJob.setTargetVersion(target);

		final SyncItemJobModel syncItemJob = modelService.create(SyncItemJobModel.class);
		syncItemJob.setCode(String.format("%s_%s", "sync_job", RandomStringUtils.randomAlphanumeric(3)));
		syncItemJob.setSourceVersion(source);
		syncItemJob.setTargetVersion(target);

		modelService.saveAll(catalogSyncItemJob);

		final CronJobHistoryModel cronJobHistoryModel1 = createEntity(catalogSyncItemJob, adminUser, baseStartDate, baseFinishDate,
				null, null);
		cronJobHistoryModelList.add(cronJobHistoryModel1);
		final CronJobHistoryModel cronJobHistoryModel2 = createEntity(syncItemJob, adminUser, baseStartDate, baseFinishDate, null,
				null);
		cronJobHistoryModelList.add(cronJobHistoryModel2);

		// when
		result = cronJobHistoryDao.findCronJobHistoryBy(null, SyncItemJobModel._TYPECODE, null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(2);
		Assertions.assertThat(result).contains(cronJobHistoryModel1, cronJobHistoryModel2);


		// when
		result = cronJobHistoryDao.findCronJobHistoryBy(null, CatalogVersionSyncJobModel._TYPECODE, null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(1);
		Assertions.assertThat(result).contains(cronJobHistoryModel1);


		// when
		result = cronJobHistoryDao.findCronJobHistoryBy(null, String.format("%s%s", SyncItemJobModel._TYPECODE, "!"), null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(1);
		Assertions.assertThat(result).contains(cronJobHistoryModel2);

		// when
		result = cronJobHistoryDao.findCronJobHistoryBy(null, JobModel._TYPECODE, null, null);

		// then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).hasSize(7);
		Assertions.assertThat(result).containsAll(cronJobHistoryModelList);


	}


	@Test(expected = IllegalArgumentException.class)
	public void testFindAllCronJobHistoryByWrongJobItemType()
	{

		// when
		cronJobHistoryDao.findCronJobHistoryBy(null, ProductModel._TYPECODE, null, null);
	}
}
