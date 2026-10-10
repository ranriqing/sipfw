<template>
  <keep-alive v-if="keepAlive">
    <router-view />
  </keep-alive>
  <router-view v-else />
</template>

<script>
  import { ROUTE } from '@/constants'

  export default {
    name: "RouteView",
    computed: {
      keepAlive () {
        return this.$route.meta[ROUTE.META_KEEPALIVE]
      }
    },
  }
</script>