package com.stockresearch.copilot.rag.vector;

import com.google.gson.JsonObject;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.config.AppProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MilvusVectorStore implements VectorStore {

	private static final String FIELD_ID = "vector_id";
	private static final String FIELD_CHUNK_ID = "chunk_id";
	private static final String FIELD_DOCUMENT_ID = "document_id";
	private static final String FIELD_COMPANY_ID = "company_id";
	private static final String FIELD_EMBEDDING = "embedding";

	private final AppProperties appProperties;
	private final int dimensions;
	private final MilvusServiceClient client;

	public MilvusVectorStore(AppProperties appProperties, int dimensions) {
		this.appProperties = appProperties;
		this.dimensions = dimensions;
		this.client = new MilvusServiceClient(ConnectParam.newBuilder()
				.withHost(appProperties.getMilvus().getHost())
				.withPort(appProperties.getMilvus().getPort())
				.build());
		ensureCollection();
	}

	@Override
	public void upsert(List<VectorRecord> records) {
		if (records == null || records.isEmpty()) {
			return;
		}
		List<String> ids = new ArrayList<>();
		List<Long> chunkIds = new ArrayList<>();
		List<Long> documentIds = new ArrayList<>();
		List<Long> companyIds = new ArrayList<>();
		List<List<Float>> embeddings = new ArrayList<>();

		for (VectorRecord record : records) {
			ids.add(record.getVectorId());
			chunkIds.add(record.getChunkId());
			documentIds.add(record.getDocumentId());
			companyIds.add(record.getCompanyId());
			List<Float> vector = new ArrayList<>(record.getEmbedding().length);
			for (float value : record.getEmbedding()) {
				vector.add(value);
			}
			embeddings.add(vector);
		}

		List<InsertParam.Field> fields = List.of(
				new InsertParam.Field(FIELD_ID, ids),
				new InsertParam.Field(FIELD_CHUNK_ID, chunkIds),
				new InsertParam.Field(FIELD_DOCUMENT_ID, documentIds),
				new InsertParam.Field(FIELD_COMPANY_ID, companyIds),
				new InsertParam.Field(FIELD_EMBEDDING, embeddings));

		R<io.milvus.grpc.MutationResult> response = client.insert(InsertParam.newBuilder()
				.withCollectionName(appProperties.getMilvus().getCollection())
				.withFields(fields)
				.build());
		assertSuccess(response, "milvus insert failed");
	}

	@Override
	public void deleteByDocumentId(Long documentId) {
		String expr = FIELD_DOCUMENT_ID + " == " + documentId;
		R<io.milvus.grpc.MutationResult> response = client.delete(DeleteParam.newBuilder()
				.withCollectionName(appProperties.getMilvus().getCollection())
				.withExpr(expr)
				.build());
		assertSuccess(response, "milvus delete failed");
	}

	@Override
	public List<VectorSearchHit> search(float[] queryEmbedding, VectorSearchFilter filter, int topK) {
		if (queryEmbedding == null || queryEmbedding.length == 0 || topK <= 0) {
			return List.of();
		}
		List<Float> vector = new ArrayList<>(queryEmbedding.length);
		for (float value : queryEmbedding) {
			vector.add(value);
		}

		SearchParam.Builder builder = SearchParam.newBuilder()
				.withCollectionName(appProperties.getMilvus().getCollection())
				.withMetricType(MetricType.COSINE)
				.withTopK(topK)
				.withVectors(List.of(vector))
				.withVectorFieldName(FIELD_EMBEDDING)
				.withOutFields(List.of(FIELD_ID, FIELD_CHUNK_ID, FIELD_DOCUMENT_ID, FIELD_COMPANY_ID));

		String expr = buildFilterExpr(filter);
		if (expr != null && !expr.isBlank()) {
			builder.withExpr(expr);
		}

		R<SearchResults> response = client.search(builder.build());
		assertSuccess(response, "milvus search failed");
		if (response.getData() == null) {
			return List.of();
		}

		SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
		List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
		List<VectorSearchHit> hits = new ArrayList<>(scores.size());
		for (int i = 0; i < scores.size(); i++) {
			SearchResultsWrapper.IDScore idScore = scores.get(i);
			Long chunkId = (Long) wrapper.getFieldData(FIELD_CHUNK_ID, 0).get(i);
			Long documentId = (Long) wrapper.getFieldData(FIELD_DOCUMENT_ID, 0).get(i);
			Long companyId = (Long) wrapper.getFieldData(FIELD_COMPANY_ID, 0).get(i);
			hits.add(VectorSearchHit.builder()
					.vectorId(String.valueOf(idScore.getStrID()))
					.chunkId(chunkId)
					.documentId(documentId)
					.companyId(companyId)
					.score(idScore.getScore())
					.build());
		}
		return hits;
	}

	private String buildFilterExpr(VectorSearchFilter filter) {
		if (filter == null) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		if (filter.getCompanyId() != null) {
			parts.add(FIELD_COMPANY_ID + " == " + filter.getCompanyId());
		}
		if (filter.getDocumentIds() != null && !filter.getDocumentIds().isEmpty()) {
			String joined = filter.getDocumentIds().stream()
					.map(String::valueOf)
					.collect(Collectors.joining(", "));
			parts.add(FIELD_DOCUMENT_ID + " in [" + joined + "]");
		}
		if (parts.isEmpty()) {
			return null;
		}
		return String.join(" && ", parts);
	}

	private void ensureCollection() {
		String collection = appProperties.getMilvus().getCollection();
		R<Boolean> has = client.hasCollection(HasCollectionParam.newBuilder()
				.withCollectionName(collection)
				.build());
		assertSuccess(has, "milvus hasCollection failed");
		if (Boolean.TRUE.equals(has.getData())) {
			client.loadCollection(LoadCollectionParam.newBuilder().withCollectionName(collection).build());
			return;
		}

		FieldType idField = FieldType.newBuilder()
				.withName(FIELD_ID)
				.withDataType(DataType.VarChar)
				.withMaxLength(64)
				.withPrimaryKey(true)
				.withAutoID(false)
				.build();
		FieldType chunkIdField = FieldType.newBuilder()
				.withName(FIELD_CHUNK_ID)
				.withDataType(DataType.Int64)
				.build();
		FieldType documentIdField = FieldType.newBuilder()
				.withName(FIELD_DOCUMENT_ID)
				.withDataType(DataType.Int64)
				.build();
		FieldType companyIdField = FieldType.newBuilder()
				.withName(FIELD_COMPANY_ID)
				.withDataType(DataType.Int64)
				.build();
		FieldType embeddingField = FieldType.newBuilder()
				.withName(FIELD_EMBEDDING)
				.withDataType(DataType.FloatVector)
				.withDimension(dimensions)
				.build();

		R<RpcStatus> create = client.createCollection(CreateCollectionParam.newBuilder()
				.withCollectionName(collection)
				.withDescription("document chunk embeddings")
				.addFieldType(idField)
				.addFieldType(chunkIdField)
				.addFieldType(documentIdField)
				.addFieldType(companyIdField)
				.addFieldType(embeddingField)
				.build());
		assertSuccess(create, "milvus createCollection failed");

		JsonObject indexParams = new JsonObject();
		indexParams.addProperty("nlist", 1024);
		R<RpcStatus> index = client.createIndex(CreateIndexParam.newBuilder()
				.withCollectionName(collection)
				.withFieldName(FIELD_EMBEDDING)
				.withIndexType(IndexType.IVF_FLAT)
				.withMetricType(MetricType.COSINE)
				.withExtraParam(indexParams.toString())
				.build());
		assertSuccess(index, "milvus createIndex failed");
		client.loadCollection(LoadCollectionParam.newBuilder().withCollectionName(collection).build());
		log.info("milvus collection ready name={} dim={}", collection, dimensions);
	}

	private <T> void assertSuccess(R<T> response, String message) {
		if (response == null || response.getStatus() != R.Status.Success.getCode()) {
			String detail = response == null ? "null" : response.getMessage();
			throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, message + ": " + detail);
		}
	}

	@PreDestroy
	public void close() {
		client.close();
	}
}
