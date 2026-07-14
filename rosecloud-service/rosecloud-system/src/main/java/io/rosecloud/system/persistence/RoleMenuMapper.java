package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMenuMapper extends BaseMapper<RoleMenuEntity> {

    @Insert("""
            <script>
            INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
            <foreach collection="rows" item="row" separator=",">
              (#{row.id}, #{row.roleId}, #{row.menuId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("rows") List<RoleMenuEntity> rows);
}
