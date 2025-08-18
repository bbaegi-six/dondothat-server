package org.bbagisix.asset.encryption;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// MyBatis TypeHandler : DB 저장 시 자동 암호화, 조회 시 자동 복호화
@Component
public class AESEncryptedTypeHandler extends BaseTypeHandler<String> implements ApplicationContextAware {

	private static EncryptionUtil encryptionUtil;
	private static ApplicationContext applicationContext;

	// Spring ApplicationContext 설정
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		AESEncryptedTypeHandler.applicationContext = applicationContext;
		AESEncryptedTypeHandler.encryptionUtil = applicationContext.getBean(EncryptionUtil.class);
	}

	// DB에 저장할 때 자동 암호화
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) {
		try {
			validateEncryptionUtil();
			String encryptedValue = encryptionUtil.encryptAES(parameter);
			ps.setString(i, encryptedValue);
		} catch (SQLException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "DB 저장 시 SQL 오류가 발생했습니다: " + err.getMessage());
		} catch (Exception err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"DB 저장 시 암호화 처리 중 예상치 못한 오류가 발생했습니다: " + err.getMessage());
		}
	}

	// DB에서 조회 할 때 자동 복호화
	@Override
	public String getNullableResult(ResultSet rs, String columnName) {
		try {
			validateEncryptionUtil();
			String encryptedValue = rs.getString(columnName);
			if (encryptedValue == null) {
				return null;
			}
			return encryptionUtil.decryptAES(encryptedValue);
		} catch (SQLException e) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"DB 조회 시 SQL 오류가 발생했습니다 (컬럼: " + columnName + "): " + e.getMessage());
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"DB 조회 시 복호화 처리 중 예상치 못한 오류가 발생했습니다 (컬럼: " + columnName + "): " + e.getMessage());
		}
	}

	// DB에서 조회할 때 자동 복호화
	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) {
		try {
			validateEncryptionUtil();
			String encryptedValue = rs.getString(columnIndex);
			if (encryptedValue == null) {
				return null;
			}
			return encryptionUtil.decryptAES(encryptedValue);
		} catch (SQLException e) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"DB 조회 시 SQL 오류가 발생했습니다 (컬럼 인덱스: " + columnIndex + "): " + e.getMessage());
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"DB 조회 시 복호화 처리 중 예상치 못한 오류가 발생했습니다 (컬럼 인덱스: " + columnIndex + "): " + e.getMessage());
		}
	}

	// CallableStatement에서 조회할 때 자동 복호화
	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) {
		try {
			validateEncryptionUtil();
			String encryptedValue = cs.getString(columnIndex);
			if (encryptedValue == null) {
				return null;
			}
			return encryptionUtil.decryptAES(encryptedValue);
		} catch (SQLException e) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"CallableStatement 조회 시 SQL 오류가 발생했습니다 (컬럼 인덱스: " + columnIndex + "): " + e.getMessage());
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"CallableStatement 조회 시 복호화 처리 중 예상치 못한 오류가 발생했습니다 (컬럼 인덱스: " + columnIndex + "): " + e.getMessage());
		}
	}

	// EncryptionUtil 주입 상태 검증
	private void validateEncryptionUtil() {
		if (encryptionUtil == null) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL,
				"EncryptionUtil이 AESEncryptedTypeHandler에 주입되지 않았습니다. Spring 설정을 확인해주세요.");
		}
	}
}