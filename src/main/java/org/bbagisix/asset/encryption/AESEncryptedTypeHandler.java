package org.bbagisix.asset.encryption;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * MyBatis TypeHandler for automatic encryption/decryption
 * DB 저장 시 자동 암호화, 조회 시 자동 복호화
 */
@Component
public class AESEncryptedTypeHandler extends BaseTypeHandler<String> implements ApplicationContextAware{

	private static EncryptionUtil encryptionUtil; // static으로 유지하여 한 번만 주입 받음
	private static ApplicationContext applicationContext; // static으로 유지하여 한 번만 주입 받음

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		// 이 메소드는 Spring이 Bean 초기화 시 딱 한 번 호출합니다.
		// 여기에서 EncryptionUtil Bean을 얻어 static 필드에 저장합니다.
		AESEncryptedTypeHandler.applicationContext = applicationContext;
		AESEncryptedTypeHandler.encryptionUtil = applicationContext.getBean(EncryptionUtil.class);
		System.out.println("AESEncryptedTypeHandler: EncryptionUtil Bean 주입 완료!");
	}

	// 🚨 중요: 인스턴스 멤버인 encryptionUtil을 사용하도록 변경!
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
		if (encryptionUtil == null) {
			throw new IllegalStateException("EncryptionUtil이 AESEncryptedTypeHandler에 주입되지 않았습니다. Spring 설정 확인 필요.");
		}
		String encryptedValue = encryptionUtil.encryptAES(parameter); // static 메소드 호출이 아닌 인스턴스 메소드 호출
		ps.setString(i, encryptedValue);
	}

	// 🚨 중요: 인스턴스 멤버인 encryptionUtil을 사용하도록 변경!
	@Override
	public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
		if (encryptionUtil == null) {
			throw new IllegalStateException("EncryptionUtil이 AESEncryptedTypeHandler에 주입되지 않았습니다. Spring 설정 확인 필요.");
		}
		String encryptedValue = rs.getString(columnName);
		if (encryptedValue == null) {
			return null;
		}
		return encryptionUtil.decryptAES(encryptedValue); // static 메소드 호출이 아닌 인스턴스 메소드 호출
	}

	// 🚨 중요: 인스턴스 멤버인 encryptionUtil을 사용하도록 변경!
	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		if (encryptionUtil == null) {
			throw new IllegalStateException("EncryptionUtil이 AESEncryptedTypeHandler에 주입되지 않았습니다. Spring 설정 확인 필요.");
		}
		String encryptedValue = rs.getString(columnIndex);
		if (encryptedValue == null) {
			return null;
		}
		return encryptionUtil.decryptAES(encryptedValue); // static 메소드 호출이 아닌 인스턴스 메소드 호출
	}

	// 🚨 중요: 인스턴스 멤버인 encryptionUtil을 사용하도록 변경!
	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		if (encryptionUtil == null) {
			throw new IllegalStateException("EncryptionUtil이 AESEncryptedTypeHandler에 주입되지 않았습니다. Spring 설정 확인 필요.");
		}
		String encryptedValue = cs.getString(columnIndex);
		if (encryptedValue == null) {
			return null;
		}
		return encryptionUtil.decryptAES(encryptedValue); // static 메소드 호출이 아닌 인스턴스 메소드 호출
	}
}