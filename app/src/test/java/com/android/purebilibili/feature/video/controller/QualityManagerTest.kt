package com.android.purebilibili.feature.video.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * QualityManager 单元测试
 * 
 * 测试覆盖:
 * - 画质权限检查
 * - 最高可用画质计算
 * - 画质标签获取
 */
class QualityManagerTest {
    
    private val qualityManager = QualityManager()
    
    // ========== checkQualityPermission 测试 ==========
    
    @Test
    @DisplayName("VIP用户应能访问所有画质")
    fun vipUserShouldAccessAllQualities() {
        // Given: VIP 用户
        val isLoggedIn = true
        val isVip = true
        
        // When & Then: 所有画质都应该返回 Permitted
        listOf(127, 126, 125, 120, 116, 112, 80, 64, 32).forEach { quality ->
            val result = qualityManager.checkQualityPermission(quality, isLoggedIn, isVip)
            assertEquals(
                QualityPermissionResult.Permitted,
                result,
                "VIP 用户应能访问 $quality 画质"
            )
        }
    }
    
    @Test
    @DisplayName("非VIP登录用户访问4K应返回RequiresVip")
    fun nonVipUserShouldGetRequiresVipFor4K() {
        // Given: 非 VIP 登录用户
        val isLoggedIn = true
        val isVip = false
        
        // When: 尝试访问 4K (120)
        val result = qualityManager.checkQualityPermission(120, isLoggedIn, isVip)
        
        // Then: 应返回 RequiresVip
        assertTrue(result is QualityPermissionResult.RequiresVip)
        assertEquals("4K", (result as QualityPermissionResult.RequiresVip).qualityLabel)
    }
    
    @Test
    @DisplayName("非VIP登录用户访问1080P60应返回RequiresVip")
    fun nonVipUserShouldGetRequiresVipFor1080P60() {
        // Given: 非 VIP 登录用户
        val isLoggedIn = true
        val isVip = false
        
        // When: 尝试访问 1080P60 (116)
        val result = qualityManager.checkQualityPermission(116, isLoggedIn, isVip)
        
        // Then: 应返回 RequiresVip
        assertTrue(result is QualityPermissionResult.RequiresVip)
        assertEquals("1080P60", (result as QualityPermissionResult.RequiresVip).qualityLabel)
    }
    
    @Test
    @DisplayName("非VIP登录用户可访问1080P")
    fun nonVipUserCanAccess1080P() {
        // Given: 非 VIP 登录用户
        val isLoggedIn = true
        val isVip = false
        
        // When: 尝试访问 1080P (80)
        val result = qualityManager.checkQualityPermission(80, isLoggedIn, isVip)
        
        // Then: 应返回 Permitted
        assertEquals(QualityPermissionResult.Permitted, result)
    }
    
    @Test
    @DisplayName("未登录用户访问1080P应返回RequiresLogin")
    fun guestUserShouldGetRequiresLoginFor1080P() {
        // Given: 未登录用户
        val isLoggedIn = false
        val isVip = false
        
        // When: 尝试访问 1080P (80)
        val result = qualityManager.checkQualityPermission(80, isLoggedIn, isVip)
        
        // Then: 应返回 RequiresLogin
        assertTrue(result is QualityPermissionResult.RequiresLogin)
        assertEquals("1080P", (result as QualityPermissionResult.RequiresLogin).qualityLabel)
    }
    
    @Test
    @DisplayName("未登录用户可访问720P及以下画质")
    fun guestUserCanAccess720PAndBelow() {
        // Given: 未登录用户
        val isLoggedIn = false
        val isVip = false
        
        // When & Then: 720P 及以下应返回 Permitted
        listOf(64, 32, 16).forEach { quality ->
            val result = qualityManager.checkQualityPermission(quality, isLoggedIn, isVip)
            assertEquals(
                QualityPermissionResult.Permitted,
                result,
                "未登录用户应能访问 $quality 画质"
            )
        }
    }
    
    // ========== getMaxAvailableQuality 测试 ==========
    
    @Test
    @DisplayName("VIP用户应获得最高可用画质")
    fun vipUserShouldGetHighestQuality() {
        // Given
        val qualities = listOf(120, 116, 80, 64, 32)
        
        // When
        val result = qualityManager.getMaxAvailableQuality(qualities, isLoggedIn = true, isVip = true)
        
        // Then
        assertEquals(120, result)
    }
    
    @Test
    @DisplayName("非VIP登录用户应获得最高非VIP画质")
    fun nonVipUserShouldGetHighestNonVipQuality() {
        // Given
        val qualities = listOf(120, 116, 80, 64, 32)
        
        // When
        val result = qualityManager.getMaxAvailableQuality(qualities, isLoggedIn = true, isVip = false)
        
        // Then: 应返回 80 (1080P)，因为 120 和 116 需要 VIP
        assertEquals(80, result)
    }
    
    @Test
    @DisplayName("未登录用户应获得最高无需登录的画质")
    fun guestUserShouldGetHighestGuestQuality() {
        // Given
        val qualities = listOf(120, 116, 80, 64, 32)
        
        // When
        val result = qualityManager.getMaxAvailableQuality(qualities, isLoggedIn = false, isVip = false)
        
        // Then: 应返回 64 (720P)，因为 80 及以上需要登录
        assertEquals(64, result)
    }
    
    @Test
    @DisplayName("空画质列表应返回默认值64")
    fun emptyQualitiesShouldReturnDefault() {
        // Given
        val qualities = emptyList<Int>()
        
        // When
        val result = qualityManager.getMaxAvailableQuality(qualities, isLoggedIn = true, isVip = true)
        
        // Then
        assertEquals(64, result)
    }
    
    // ========== 画质标签测试 ==========
    
    @Test
    @DisplayName("应返回正确的画质标签")
    fun shouldReturnCorrectQualityLabels() {
        assertEquals("4K", qualityManager.getQualityLabel(120))
        assertEquals("1080P60", qualityManager.getQualityLabel(116))
        assertEquals("1080P", qualityManager.getQualityLabel(80))
        assertEquals("720P", qualityManager.getQualityLabel(64))
    }
    
    // ========== requiresVip / requiresLogin 测试 ==========
    
    @Test
    @DisplayName("VIP画质阈值应为112")
    fun vipThresholdShouldBe112() {
        assertTrue(qualityManager.requiresVip(112))
        assertTrue(qualityManager.requiresVip(120))
        assertTrue(!qualityManager.requiresVip(80))
    }
    
    @Test
    @DisplayName("登录画质阈值应为80")
    fun loginThresholdShouldBe80() {
        assertTrue(qualityManager.requiresLogin(80))
        assertTrue(qualityManager.requiresLogin(112))
        assertTrue(!qualityManager.requiresLogin(64))
    }
}
