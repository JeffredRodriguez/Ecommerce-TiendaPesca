package com.tiendapesca.APItiendapesca.Repository;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tiendapesca.APItiendapesca.Entities.OrderDetail;



@Repository
public interface OrderDetail_Repository extends JpaRepository<OrderDetail, Integer> {
}