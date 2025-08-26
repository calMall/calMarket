"use client";

import { addeAddress, deleteAddress, getMyInfo } from "@/api/User";
import UserStore from "@/store/user";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import DeliveryMenu from "./DeliveryMenu";
import AddAddress from "./AddAddress";

interface props {
  setViewModal: React.Dispatch<React.SetStateAction<boolean>>;
  setSelectedAddress: React.Dispatch<
    React.SetStateAction<deliveryAddressDetail | null>
  >;
  setMyInfo: React.Dispatch<React.SetStateAction<MyinfoDTO | null>>;
  initialDeliveryAddress?: deliveryAddressDetail[];
  selectedAddress: deliveryAddressDetail | null;
}

export default function DeliveryAddressModal({
  setViewModal,
  setSelectedAddress,
  initialDeliveryAddress,
  selectedAddress,
  setMyInfo,
}: props) {
  const [deliveryAddresses, setDeliveryAddresses] = useState<
    deliveryAddressDetail[]
  >(initialDeliveryAddress || []);

  const ref = useRef<HTMLDivElement>(null);
  const userStore = UserStore();
  const router = useRouter();
  const [isViewAddAddressForm, setIsViewAddAddressForm] = useState(false);
  const onDelete = async (address: deliveryAddressDetail) => {
    if (window.confirm("住所を削除しますか？")) {
      try {
        const data = await deleteAddress(address);
        if (data.message === "success") {
          refreshData();
        }
      } catch (e: any) {
        if (e.status === 401) {
          alert("ログインが必要です。ログインページに移動します。");
          userStore.logout();
          router.push("/login");
        } else {
          alert("エラーが発生しました。");
        }
      }
    }
  };

  const refreshData = async () => {
    try {
      const data = await getMyInfo();
      if (data.message === "success") {
        setDeliveryAddresses(data.deliveryAddressDetails);
        setMyInfo(data);
        data.deliveryAddressDetails.length > 0
          ? setSelectedAddress(data.deliveryAddressDetails[0])
          : setSelectedAddress(null);
      }
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        router.push("/login");
      } else {
        alert("エラーが発生しました。");
      }
    }
  };

  const onAddAddress = async (address: deliveryAddressDetail) => {
    if (deliveryAddresses.length >= 3) {
      return alert("住所は3件まで追加できます。");
    }
    const postalCodeRegex = /^\d{3}-\d{4}$/;

    if (!postalCodeRegex.test(address.postalCode)) {
      return alert("正しい日本の郵便番号を入力してください。(例: 123-4567)");
    }
    if (!address.address1) {
      return alert("詳細住所を入力してください。");
    }
    try {
      const data = await addeAddress(address);
      if (data.message === "success") {
        await refreshData();
        setIsViewAddAddressForm(false);
      }
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        router.push("/login");
      } else {
        alert("エラーが発生しました。");
      }
    }
  };

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setViewModal(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [ref, setViewModal]);

  useEffect(() => {}, []);

  return (
    <div className="address-box" ref={ref}>
      <div className="flex ae jb">
        <h3>お届け先住所</h3>
        <button
          onClick={() => setIsViewAddAddressForm(true)}
          className="cart-del-btn"
        >
          住所追加
        </button>
      </div>
      {isViewAddAddressForm && (
        <AddAddress setIsView={setIsViewAddAddressForm} onFunc={onAddAddress} />
      )}
      <div className="address-list mt-1">
        {deliveryAddresses.map((address) => (
          <div
            className="flex container"
            key={address.postalCode + address.address1}
          >
            <button
              className="address-item flex ac gap-1 wf"
              onClick={() => setSelectedAddress(address)}
            >
              <input
                type="radio"
                checked={
                  (selectedAddress && JSON.stringify(selectedAddress)) ===
                  JSON.stringify(address)
                }
                onChange={() => setSelectedAddress(address)}
              />
              <div className="wf">
                <div className="fw-600">{address.postalCode}</div>
                <div>{address.address1}</div>
              </div>
            </button>
            <DeliveryMenu address={address} onDelete={onDelete} />
          </div>
        ))}
      </div>
    </div>
  );
}
