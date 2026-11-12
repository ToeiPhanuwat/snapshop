import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { SHOP_TYPE_LABEL } from "@/lib/format";
import type { ShopType } from "@/types/api";

const TONE: Record<ShopType, BadgeTone> = { MALL: "danger", PREFERRED: "brand", REGULAR: "neutral" };

export function ShopTypeBadge({ shopType }: { shopType: ShopType }) {
  return <Badge tone={TONE[shopType]}>{SHOP_TYPE_LABEL[shopType]}</Badge>;
}
