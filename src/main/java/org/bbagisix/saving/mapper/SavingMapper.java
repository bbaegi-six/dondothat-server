package org.bbagisix.saving.mapper;

import java.util.List;

import org.bbagisix.saving.dto.SavingDTO;

public interface SavingMapper {
	List<SavingDTO> getSavingHistory(Long userId);

	Long getTotalSaving(Long userId);
}
